package com.example.application.classes.repository;

import com.example.application.classes.model.Client;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClientRepository {

    private final DataSource dataSource;

    public ClientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Long> insert(long companyId, Client client) throws SQLException {
        final String sql = """
            INSERT INTO client (
                company_id,
                name,
                email,
                phone,
                notes,
                doc_type,
                document,
                created_by_user_id,
                cep,
                uf,
                city,
                district,
                street,
                number,
                complement
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, version, creation_date, update_date
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setString(2, client.getName());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getPhone());
            ps.setString(5, client.getNotes());
            ps.setString(6, client.getDocType());
            ps.setString(7, client.getDocument());
            setLongOrNull(ps, 8, client.getCreatedByUserId());
            ps.setString(9, client.getCep());
            ps.setString(10, client.getUf());
            ps.setString(11, client.getCity());
            ps.setString(12, client.getDistrict());
            ps.setString(13, client.getStreet());
            ps.setString(14, client.getNumber());
            ps.setString(15, client.getComplement());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                client.setId(rs.getLong("id"));
                client.setVersion(rs.getInt("version"));
                client.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                client.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return Optional.of(client.getId());
            }
        }
    }

    public Optional<Client> findById(long companyId, long id) throws SQLException {
        final String sql = baseSelect() + """
            WHERE company_id = ?
              AND id = ?
              AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setLong(2, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Client> listByCompany(long companyId) throws SQLException {
        final String sql = baseSelect() + """
            WHERE company_id = ?
              AND deleted_at IS NULL
            ORDER BY id
            """;

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

    public boolean existsByCompanyAndDocument(long companyId,
                                              String docType,
                                              String document,
                                              Long ignoreId) throws SQLException {

        final String sql = """
            SELECT 1
              FROM client
             WHERE company_id = ?
               AND doc_type = ?
               AND document = ?
               AND deleted_at IS NULL
               """ + (ignoreId != null ? " AND id <> ?" : "") + " LIMIT 1";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setString(2, docType);
            ps.setString(3, document);
            if (ignoreId != null) ps.setLong(4, ignoreId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean updateBasics(long companyId, Client client) throws SQLException {
        final String sql = """
            UPDATE client
               SET name = ?,
                   email = ?,
                   phone = ?,
                   notes = ?,
                   doc_type = ?,
                   document = ?,
                   cep = ?,
                   uf = ?,
                   city = ?,
                   district = ?,
                   street = ?,
                   number = ?,
                   complement = ?,
                   update_date = NOW(),
                   version = version + 1
             WHERE id = ?
               AND version = ?
               AND company_id = ?
               AND deleted_at IS NULL
            RETURNING version, update_date
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, client.getName());
            ps.setString(2, client.getEmail());
            ps.setString(3, client.getPhone());
            ps.setString(4, client.getNotes());
            ps.setString(5, client.getDocType());
            ps.setString(6, client.getDocument());
            ps.setString(7, client.getCep());
            ps.setString(8, client.getUf());
            ps.setString(9, client.getCity());
            ps.setString(10, client.getDistrict());
            ps.setString(11, client.getStreet());
            ps.setString(12, client.getNumber());
            ps.setString(13, client.getComplement());
            ps.setLong(14, client.getId());
            ps.setInt(15, client.getVersion());
            ps.setLong(16, client.getCompanyId());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                client.setVersion(rs.getInt("version"));
                client.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return true;
            }
        }
    }


    public boolean softDelete(long companyId, long id) throws SQLException {
        final String sql = """
            UPDATE client
               SET deleted_at = NOW(),
                   update_date = NOW(),
                   version = version + 1
             WHERE company_id = ?
               AND id = ?
               AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static String baseSelect() {
        return """
            SELECT id, version, creation_date, update_date,
                   company_id, name, email, phone, notes, deleted_at,
                   doc_type, document, created_by_user_id,
                   cep, uf, city, district, street, number, complement
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
        c.setDeletedAt(rs.getTimestamp("deleted_at") != null
                ? rs.getTimestamp("deleted_at").toLocalDateTime()
                : null);
        c.setDocType(rs.getString("doc_type"));
        c.setDocument(rs.getString("document"));
        c.setCreatedByUserId(rs.getObject("created_by_user_id", Long.class));
        c.setCep(rs.getString("cep"));
        c.setUf(rs.getString("uf"));
        c.setCity(rs.getString("city"));
        c.setDistrict(rs.getString("district"));
        c.setStreet(rs.getString("street"));
        c.setNumber(rs.getString("number"));
        c.setComplement(rs.getString("complement"));
        return c;
    }

    private static void setLongOrNull(PreparedStatement ps, int idx, Long v) throws SQLException {
        if (v != null) ps.setLong(idx, v);
        else ps.setNull(idx, Types.BIGINT);
    }
}