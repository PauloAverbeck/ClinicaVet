package com.example.application.classes.repository;

import com.example.application.classes.model.AppUser;
import com.example.application.classes.service.CompanyUserRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AppUserRepository {

    private final DataSource dataSource;

    @Autowired
    public AppUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* CREATE */
     /** Autocadastro: insere usuário com (opcional) senha provisória já hasheada. */

    public long insertWithProvisional(AppUser user) throws SQLException {
        final String sql = """
            INSERT INTO app_user (email, name, password_hash, prov_pw_hash, email_conf_time)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, creation_date, update_date, version
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getName());

            String pwd = user.getPasswordHash();
            String prov = user.getProvisionalPasswordHash();
            if (pwd == null && prov == null) {
                throw new SQLException("Ao menos um hash de senha deve ser fornecido (password_hash ou prov_pw_hash).");
            }
            if (pwd == null) pwd = prov;

            ps.setString(3, pwd);
            setStringOrNull(ps, 4, prov);
            setLocalDateTimeOrNull(ps, 5, user.getEmailConfirmationTime());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong("id");
                user.setId(id);
                user.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                user.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                user.setVersion(rs.getInt("version"));
                return id;
            }
        }
    }

    /* READ */

    public Optional<AppUser> findById(long id) throws SQLException {
        final String sql = baseSelect() + " WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public Optional<AppUser> findByEmail(String email) throws SQLException {
        final String sql = baseSelect() + " WHERE LOWER(email) = LOWER(?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        final String sql = "SELECT 1 FROM app_user WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<CompanyUserRow> listCompanyUsers(long companyId) throws SQLException {
        final String sql = """
            SELECT au.id AS user_id,
                   au.name AS name,
                   au.email AS email,
                   uc.admin AS admin
              FROM user_company uc
              JOIN app_user au ON uc.user_id = au.id
             WHERE uc.company_id = ?
                AND uc.deleted_at IS NULL
             ORDER BY au.name
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CompanyUserRow> users = new ArrayList<>();
                while (rs.next()) {
                    CompanyUserRow row = new CompanyUserRow(
                        rs.getLong("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getBoolean("admin")
                    );
                    users.add(row);
                }
                return users;
            }
        }
    }

    public List<AppUser> listAll() throws SQLException {
        final String sql = baseSelect() + " ORDER BY name";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<AppUser> users = new ArrayList<>();
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        }
    }

    /* UPDATE (basico) */
    /**
     * Atualiza apenas dados não sensíveis (email, nome).
     * Não toca em senhas nem em email_conf_time.
     * Usa optimistic locking por version.
     */
    public void updateDadosBasicos(AppUser user) throws SQLException {
        final String sql = """
            UPDATE app_user
               SET email = ?,
                   name  = ?,
                   version = version + 1
             WHERE id = ? AND version = ?
             RETURNING update_date, version
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getName());
            ps.setLong(3, user.getId());
            ps.setInt(4, user.getVersion());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                    user.setVersion(rs.getInt("version"));
                } else {
                    throw new SQLException("Conflito de versão para AppUser id=" + user.getId());
                }
            }
        }
    }

    /* UPDATE (senhas e confirmação) */
    /** Define uma nova senha provisória (esqueci minha senha). Zera a confirmação. */
    public void setProvisional(long userId, String provisionalHash) throws SQLException {
        final String sql = """
            UPDATE app_user
               SET prov_pw_hash = ?,
                   version = version + 1,
                   update_date = CURRENT_TIMESTAMP
             WHERE id = ?
             RETURNING update_date, version
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, provisionalHash);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Falha ao definir senha provisória para AppUser id=" + userId);
                }
            }
        }
    }

    /**
     * Promove a senha provisória para oficial e confirma o email.
     * (Usado no primeiro login/fluxo de confirmação.)
     */
    public boolean promoteProvisionalToOfficial(long userId) throws SQLException {
        final String sql = """
        UPDATE app_user
           SET password_hash   = prov_pw_hash,
               prov_pw_hash    = NULL,
               email_conf_time = COALESCE(email_conf_time, CURRENT_TIMESTAMP),
               update_date     = CURRENT_TIMESTAMP,
               version         = version + 1
         WHERE id = ?
           AND prov_pw_hash IS NOT NULL
    """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Atualiza a senha oficial (não mexe na provisória). */
    public void updatePassword(long userId, String newHash, int currentVersion) throws SQLException {
        final String sql = """
            UPDATE app_user
               SET password_hash = ?,
                   version = version + 1,
                   update_date = CURRENT_TIMESTAMP
             WHERE id = ? AND version = ?
             RETURNING update_date, version
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.setInt(3, currentVersion);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Conflito de versão ao atualizar senha (AppUser id=" + userId + ")");
                }
            }
        }
    }

    public void updateOfficialPasswordAndClearProvisional(long userId, String newHash) throws SQLException {
        String sql = """
        UPDATE app_user
           SET password_hash = ?,
               prov_pw_hash = NULL,
               email_conf_time = CURRENT_TIMESTAMP,
               update_date = CURRENT_TIMESTAMP,
               version = version + 1
         WHERE id = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /* SAVE & DELETE */
    /**
     * Salva um usuário (insert ou update) de forma unificada.
     * Se o ID for nulo (0), insere; senão, atualiza dados básicos.
     */
    public void save(AppUser user) throws SQLException {
        if (user.getId() == 0) {
            insertWithProvisional(user);
        } else {
            updateDadosBasicos(user);
        }
    }

    /**
     * Remove permanentemente um usuário pelo ID.
     * Hard delete.
     */
    public void deleteById(long id) throws SQLException {
        final String sql = """
            DELETE FROM app_user
             WHERE id = ?
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Nenhum AppUser encontrado para exclusão. ID=" + id);
            }
        }
    }

    /* HELPERS */

    private static String baseSelect() {
        return """
            SELECT id, creation_date, update_date, version,
                   email, name, password_hash, prov_pw_hash, email_conf_time
              FROM app_user
            """;
    }

    private static void setLocalDateTimeOrNull(PreparedStatement ps, int index, LocalDateTime localDateTime) throws SQLException {
        if (localDateTime != null) {
            ps.setTimestamp(index, Timestamp.valueOf(localDateTime));
        } else {
            ps.setNull(index, Types.TIMESTAMP);
        }
    }

    private static void setStringOrNull(PreparedStatement ps, int index, String strValue) throws SQLException {
        if (strValue != null) {
            ps.setString(index, strValue);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    private static AppUser map(ResultSet rs) throws SQLException {
        AppUser user = new AppUser();
        user.setId(rs.getLong("id"));
        user.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        user.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        user.setVersion(rs.getInt("version"));
        user.setEmail(rs.getString("email"));
        user.setName(rs.getString("name"));
        user.setPasswordHash(rs.getString("password_hash"));

        String provisionalPasswordHash = rs.getString("prov_pw_hash");
        if (rs.wasNull()) {
            provisionalPasswordHash = null;
        }
        user.setProvisionalPasswordHash(provisionalPasswordHash);

        Timestamp emailConfirmationTimeTs = rs.getTimestamp("email_conf_time");
        user.setEmailConfirmationTime(emailConfirmationTimeTs != null ? emailConfirmationTimeTs.toLocalDateTime() : null);
        return user;
    }
}
