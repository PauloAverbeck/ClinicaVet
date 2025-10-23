package com.example.application.classes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserCompanyRepository {
    private final DataSource dataSource;

    @Autowired
    public UserCompanyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* CREATE */

    public long insert (long createdByUserId, long userId, long companyId, boolean admin) throws SQLException {
        final String sql = """
                INSERT INTO user_company (created_by_user_id, user_id, company_id, admin)
                VALUES (?, ?, ?, ?)
                RETURNING id, creation_date, update_date, version
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, createdByUserId);
            ps.setLong(2, userId);
            ps.setLong(3, companyId);
            ps.setBoolean(4, admin);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }

    /* READ */

    public Optional<UserCompanyLink> findActive(long userId, long companyId) throws SQLException {
        final String sql = baseSelect() + " WHERE uc.user_id=? AND uc.company_id=? AND uc.deleted_at IS NULL";
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public boolean isUserAdmin(long userId, long companyId) throws SQLException {
        final String sql = """
                SELECT 1 FROM user_company
                WHERE user_id=? AND company_id=? AND deleted_at IS NULL AND admin = TRUE
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

    public List<UserCompanyLink> listActiveByUser(long userId) throws SQLException {
        final String sql = baseSelect() + " WHERE uc.user_id=? AND uc.deleted_at IS NULL ORDER BY c.name";
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<UserCompanyLink> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<UserCompanyLink> listActiveByCompany(long companyId) throws SQLException {
        final String sql = baseSelect() + " WHERE uc.company_id=? AND uc.deleted_at IS NULL ORDER BY u.name";
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

    /* UPDATE */

    public void setAdmin(long userId, long companyId, boolean admin) throws SQLException {
        final String sql = """
                UPDATE user_company
                   SET admin = ?,
                       version = version + 1
                WHERE user_id = ? AND company_id = ? AND deleted_at IS NULL
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, companyId);
            ps.setBoolean(3, admin);
            ps.executeUpdate();
        }
    }

    public void restore(long userId, long companyId) throws SQLException {
        final String sql = """
                UPDATE user_company
                   SET deleted_at = NULL,
                       version = version + 1
                WHERE user_id = ? AND company_id = ?
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, companyId);
            ps.executeUpdate();
        }
    }

    /* DELETE */

    public void softDelete(long userId, long companyId) throws SQLException {
        final String sql = """
                UPDATE user_company
                   SET deleted_at = CURRENT_TIMESTAMP,
                       version = version + 1
                WHERE user_id = ? AND company_id = ? AND deleted_at IS NULL
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, companyId);
            ps.executeUpdate();
        }
    }

    /* HELPERS */

    private static String baseSelect() {
        return """
                SELECT uc.id, uc.version, uc.creation_date, uc.update_date,
                       uc.user_id, uc.company_id, uc.created_by_user_id, uc.admin, uc.deleted_at
                FROM user_company uc
                """;
    }

    private static UserCompanyLink map (ResultSet rs) throws SQLException {
        UserCompanyLink link = new UserCompanyLink();
        link.setId(rs.getLong("id"));
        link.setVersion(rs.getInt("version"));
        link.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        link.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        link.setUserId(rs.getLong("user_id"));
        link.setCompanyId(rs.getLong("company_id"));
        link.setCreatedByUserId(rs.getLong("created_by_user_id"));
        link.setAdmin(rs.getBoolean("admin"));
        Timestamp del = rs.getTimestamp("deleted_at");
        link.setDeletedAt(del != null ? del.toLocalDateTime() : null);
        return link;
    }
}
