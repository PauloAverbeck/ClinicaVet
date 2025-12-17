package com.example.application.classes.repository;

import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.service.CompanyChoice;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Repository
public class UserCompanyRepository {

    private final DataSource dataSource;

    public UserCompanyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Garante vínculo ativo (restaura se existia deletado; cria se não existia).
     * Se já existir ativo, retorna o id do vínculo ativo.
     */
    public long insertOrRestore(long createdByUserId, long userId, long companyId, boolean admin) throws SQLException {

        Optional<Long> activeId = findActiveId(userId, companyId);
        if (activeId.isPresent()) {
            return activeId.get();
        }

        final String restoreSql = """
            UPDATE user_company
               SET deleted_at = NULL,
                   admin = ?,
                   update_date = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE user_id = ?
               AND company_id = ?
               AND deleted_at IS NOT NULL
             RETURNING id
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(restoreSql)) {

            ps.setBoolean(1, admin);
            ps.setLong(2, userId);
            ps.setLong(3, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }

        final String insertSql = """
            INSERT INTO user_company (created_by_user_id, user_id, company_id, admin, deleted_at)
            VALUES (?, ?, ?, ?, NULL)
            RETURNING id
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(insertSql)) {

            ps.setLong(1, createdByUserId);
            ps.setLong(2, userId);
            ps.setLong(3, companyId);
            ps.setBoolean(4, admin);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }

        } catch (SQLException ex) {
            // 23505 = unique violation
            if ("23505".equals(ex.getSQLState())) {
                Optional<Long> again = findActiveId(userId, companyId);
                if (again.isPresent()) return again.get();
            }
            throw ex;
        }
    }

    public Optional<UserCompanyLink> findActive(long userId, long companyId) throws SQLException {
        final String sql = baseSelect() + """
            WHERE uc.user_id = ?
              AND uc.company_id = ?
              AND uc.deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean isUserAdmin(long userId, long companyId) throws SQLException {
        final String sql = """
            SELECT 1
              FROM user_company
             WHERE user_id = ?
               AND company_id = ?
               AND deleted_at IS NULL
               AND admin = TRUE
             LIMIT 1
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Map<Long, String> companiesByUserIdAggregated() throws SQLException {
        final String sql = """
            SELECT u.id AS user_id,
                   COALESCE(string_agg(DISTINCT c.name, ', ' ORDER BY c.name), '') AS companies
              FROM app_user u
              LEFT JOIN user_company uc
                ON uc.user_id = u.id
               AND uc.deleted_at IS NULL
              LEFT JOIN company c
                ON c.id = uc.company_id
               AND c.deleted_at IS NULL
             GROUP BY u.id
            """;

        Map<Long, String> out = new HashMap<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.put(rs.getLong("user_id"), rs.getString("companies"));
            }
        }
        return out;
    }

    public List<CompanyChoice> listActiveCompanyChoicesByUser(long userId) throws SQLException {
        final String sql = """
            SELECT uc.company_id,
                   c.name,
                   uc.admin
              FROM user_company uc
              JOIN company c ON c.id = uc.company_id
             WHERE uc.user_id = ?
               AND uc.deleted_at IS NULL
               AND c.deleted_at IS NULL
             ORDER BY c.name
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                List<CompanyChoice> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new CompanyChoice(
                            rs.getLong("company_id"),
                            rs.getString("name"),
                            rs.getBoolean("admin")
                    ));
                }
                return list;
            }
        }
    }

    public List<UserCompanyLink> listActiveLinksByUser(long userId) throws SQLException {
        final String sql = baseSelect() + """
            WHERE uc.user_id = ?
              AND uc.deleted_at IS NULL
            ORDER BY uc.company_id
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                List<UserCompanyLink> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<UserCompanyLink> listActiveByCompany(long companyId) throws SQLException {
        final String sql = baseSelect() + """
            JOIN app_user u ON u.id = uc.user_id
            WHERE uc.company_id = ?
              AND uc.deleted_at IS NULL
            ORDER BY u.name
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                List<UserCompanyLink> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public boolean setAdmin(long userId, long companyId, boolean admin) throws SQLException {
        final String sql = """
            UPDATE user_company
               SET admin = ?,
                   update_date = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE user_id = ?
               AND company_id = ?
               AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, admin);
            ps.setLong(2, userId);
            ps.setLong(3, companyId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean restore(long userId, long companyId) throws SQLException {
        final String sql = """
            UPDATE user_company
               SET deleted_at = NULL,
                   update_date = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE user_id = ?
               AND company_id = ?
               AND deleted_at IS NOT NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, companyId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean softDelete(long userId, long companyId) throws SQLException {
        final String sql = """
            UPDATE user_company
               SET deleted_at = CURRENT_TIMESTAMP,
                   update_date = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE user_id = ?
               AND company_id = ?
               AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, companyId);
            return ps.executeUpdate() == 1;
        }
    }

    private Optional<Long> findActiveId(long userId, long companyId) throws SQLException {
        final String sql = """
            SELECT id
              FROM user_company
             WHERE user_id = ?
               AND company_id = ?
               AND deleted_at IS NULL
             LIMIT 1
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(rs.getLong("id"));
            }
        }
    }

    private static String baseSelect() {
        return """
            SELECT uc.id, uc.version, uc.creation_date, uc.update_date,
                   uc.user_id, uc.company_id, uc.created_by_user_id, uc.admin, uc.deleted_at
              FROM user_company uc
            """;
    }

    private static UserCompanyLink map(ResultSet rs) throws SQLException {
        UserCompanyLink link = new UserCompanyLink();
        link.setId(rs.getLong("id"));
        link.setVersion(rs.getInt("version"));
        link.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        link.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        link.setUserId(rs.getLong("user_id"));
        link.setCompanyId(rs.getLong("company_id"));

        Long createdBy = rs.getObject("created_by_user_id", Long.class);
        link.setCreatedByUserId(createdBy);

        link.setAdmin(rs.getBoolean("admin"));

        Timestamp del = rs.getTimestamp("deleted_at");
        link.setDeletedAt(del != null ? del.toLocalDateTime() : null);

        return link;
    }
}