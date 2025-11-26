package com.example.application.classes.repository;

import com.example.application.classes.model.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepository {

    private final DataSource dataSource;

    @Autowired
    public ClientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* CREATE */

    public long insert(Client client) throws SQLException {
        final String sql = """
                INSERT INTO client (company_id, name, email, phone, notes)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, version, creation_date, update_date
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, client.getCompanyId());
            ps.setString(2, client.getName());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getPhone());
            ps.setString(5, client.getNotes());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong("id");
                client.setId(id);
                client.setVersion(rs.getInt("version"));
                client.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                client.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return id;
            }
        }
    }

    /* READ */

    public Optional<Client> findById(long companyId, long id) throws SQLException {
        final String sql = baseSelect() +
                " WHERE company_id = ? AND id = ? AND deleted_at IS NULL";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setLong(2, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        }
    }

    public List<Client> listByCompany(long companyId) throws SQLException {
        final String sql = baseSelect() +
                " WHERE company_id = ? AND deleted_at IS NULL ORDER BY name";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Client> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public List<Client> searchByNameOrEmail(long companyId, String query, int limit) throws SQLException {
        final String sql = baseSelect() + """
                WHERE company_id = ?
                  AND deleted_at IS NULL
                  AND (
                        LOWER(name)  LIKE LOWER(?)
                     OR LOWER(email) LIKE LOWER(?)
                  )
                ORDER BY name
                LIMIT ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + query + "%";
            ps.setLong(1, companyId);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt(4, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<Client> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    /* UPDATE */

    public void updateBasics(Client client) throws SQLException {
        final String sql = """
                UPDATE client
                   SET name   = ?,
                       email  = ?,
                       phone  = ?,
                       notes  = ?,
                       update_date = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                   AND company_id = ?
                   AND deleted_at IS NULL
                   AND version = ?
                 RETURNING update_date, version
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, client.getName());
            ps.setString(2, client.getEmail());
            ps.setString(3, client.getPhone());
            ps.setString(4, client.getNotes());
            ps.setLong(5, client.getId());
            ps.setLong(6, client.getCompanyId());
            ps.setInt(7, client.getVersion());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime upd = rs.getTimestamp("update_date").toLocalDateTime();
                    client.setUpdateDate(upd);
                    client.setVersion(rs.getInt("version"));
                } else {
                    throw new SQLException("Conflito de versão ou cliente não encontrado. id=" + client.getId());
                }
            }
        }
    }

    /* DELETE (soft) */

    public void softDelete(long companyId, long id) throws SQLException {
        final String sql = """
                UPDATE client
                   SET deleted_at = CURRENT_TIMESTAMP,
                       update_date = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE company_id = ?
                   AND id = ?
                   AND deleted_at IS NULL
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setLong(2, id);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Cliente não encontrado ou já deletado. id=" + id);
            }
        }
    }

    /* HELPERS */

    private static String baseSelect() {
        return """
                SELECT id,
                       version,
                       creation_date,
                       update_date,
                       company_id,
                       name,
                       email,
                       phone,
                       notes,
                       deleted_at
                  FROM client
                """;
    }

    private static Client map(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getLong("id"));
        c.setVersion(rs.getInt("version"));
        c.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        c.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        c.setCompanyId(rs.getLong("company_id"));
        c.setName(rs.getString("name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setNotes(rs.getString("notes"));

        Timestamp del = rs.getTimestamp("deleted_at");
        c.setDeletedAt(del != null ? del.toLocalDateTime() : null);

        return c;
    }
}