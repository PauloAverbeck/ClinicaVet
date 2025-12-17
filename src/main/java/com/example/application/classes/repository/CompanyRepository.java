package com.example.application.classes.repository;

import com.example.application.classes.DocumentType;
import com.example.application.classes.model.Company;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CompanyRepository {

    private final DataSource dataSource;

    public CompanyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Long> insert(Company company) throws SQLException {
        final String sql = """
            INSERT INTO company (name, document_type, document)
            VALUES (?, ?, ?)
            RETURNING id, creation_date, update_date, version
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, company.getName());
            ps.setString(2, company.getDocumentType() != null ? company.getDocumentType().name() : null);
            ps.setString(3, company.getDocument());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                long id = rs.getLong("id");
                company.setId(id);
                company.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                company.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                company.setVersion(rs.getInt("version"));

                return Optional.of(id);
            }
        }
    }

    public Optional<Company> findById(long id) throws SQLException {
        final String sql = baseSelect() + """
            WHERE id = ?
              AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Company> findByDocument(DocumentType type, String document) throws SQLException {
        final String sql = baseSelect() + """
            WHERE document_type = ?
              AND document = ?
              AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, type != null ? type.name() : null);
            ps.setString(2, document);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByDocument(DocumentType type, String document, Long ignoreCompanyId) throws SQLException {
        final String sql = """
            SELECT 1
              FROM company
             WHERE document_type = ?
               AND document = ?
               AND deleted_at IS NULL
            """ + (ignoreCompanyId != null ? " AND id <> ?" : "") + " LIMIT 1";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, type != null ? type.name() : null);
            ps.setString(2, document);
            if (ignoreCompanyId != null) ps.setLong(3, ignoreCompanyId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Company> listAll() throws SQLException {
        final String sql = baseSelect() + """
            WHERE deleted_at IS NULL
            ORDER BY id
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Company> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Company> searchByName(String name, int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, 200));

        final String sql = baseSelect() + """
            WHERE deleted_at IS NULL
              AND LOWER(name) LIKE LOWER(?)
            ORDER BY name
            LIMIT ?
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + (name == null ? "" : name.trim()) + "%");
            ps.setInt(2, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                List<Company> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public boolean updateBasics(Company company) throws SQLException {
        final String sql = """
            UPDATE company
               SET name = ?,
                   document_type = ?,
                   document = ?,
                   update_date = NOW(),
                   version = version + 1
             WHERE id = ?
               AND version = ?
               AND deleted_at IS NULL
            RETURNING update_date, version
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, company.getName());
            ps.setString(2, company.getDocumentType() != null ? company.getDocumentType().name() : null);
            ps.setString(3, company.getDocument());
            ps.setLong(4, company.getId());
            ps.setInt(5, company.getVersion());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                company.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                company.setVersion(rs.getInt("version"));
                return true;
            }
        }
    }

    public boolean softDeleteById(long id) throws SQLException {
        final String sql = """
            UPDATE company
               SET deleted_at = NOW(),
                   update_date = NOW(),
                   version = version + 1
             WHERE id = ?
               AND deleted_at IS NULL
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static String baseSelect() {
        return """
            SELECT id, version, creation_date, update_date,
                   name, document_type, document, deleted_at
              FROM company
            """;
    }

    private static Company map(ResultSet rs) throws SQLException {
        Company c = new Company();
        c.setId(rs.getLong("id"));
        c.setVersion(rs.getInt("version"));
        c.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        c.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        c.setName(rs.getString("name"));
        c.setDocumentType(DocumentType.fromString(rs.getString("document_type")));
        c.setDocument(rs.getString("document"));
        return c;
    }
}