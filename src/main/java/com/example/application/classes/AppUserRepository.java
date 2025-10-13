package com.example.application.classes;

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

    public long insert(AppUser user) throws SQLException {
        final String sql = """
                INSERT INTO app_user (email, name, password_hash, prov_pw_hash, email_conf_time, version)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id, creation_date, update_date, version
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setLocalDateTimeOrNull(ps, 1, user.getEmailConfirmationTime());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getName());
            ps.setString(4, user.getPasswordHash());
            setStringOrNull(ps, 5, user.getProvisionalPasswordHash());

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

    public void update(AppUser user) throws SQLException {
        final String sql = """
                    UPDATE app_user
                        SET email = ?,
                            name = ?,
                            password_hash = ?,
                            prov_pw_hash = ?,
                            email_conf_time = ?,
                            update_date = ?,
                            version = version + 1
                    WHERE id = ? AND version = ?
                    RETURNING update_date, version
                    """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setLocalDateTimeOrNull(ps, 1, user.getEmailConfirmationTime());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getName());
            ps.setString(4, user.getPasswordHash());
            setStringOrNull(ps, 5, user.getProvisionalPasswordHash());

            ps.setLong(6, user.getId());
            ps.setInt(7, user.getVersion());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                    user.setVersion(rs.getInt("version"));
                } else {
                    throw new SQLException("Conflito de versão para AppUser com id: " + user.getId());
                }
            }
        }
    }

    public Optional<AppUser> findById(long id) throws SQLException {
        final String sql = baseSelect() + " WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    public Optional<AppUser> findByEmail(String email) throws SQLException {
        final String sql = baseSelect() + " WHERE email = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                } else {
                    return Optional.empty();
                }
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

    /* Helpers */
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
        if (emailConfirmationTimeTs != null) {
            user.setEmailConfirmationTime(emailConfirmationTimeTs.toLocalDateTime());
        } else {
            user.setEmailConfirmationTime(null);
        }
        return user;
    }
}
