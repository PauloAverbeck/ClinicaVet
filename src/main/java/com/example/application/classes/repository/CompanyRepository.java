package com.example.application.classes.repository;

import com.example.application.classes.DocumentType;
import com.example.application.classes.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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

    @Autowired
    public CompanyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long insert(Company company) throws SQLException {
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
                rs.next();
                long id = rs.getLong("id");
                company.setId(id);
                company.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                company.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                company.setVersion(rs.getInt("version"));
                return id;
            }
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState()) || String.valueOf(ex.getMessage()).toLowerCase().contains("unique")) {
                throw new DuplicateKeyException("Documento já cadastrado para outra empresa.", ex);
            }
            throw ex;
        }
    }

    public Optional<Company> findById(long id) throws SQLException {
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

    public Optional<Company> findByDocument(DocumentType type, String document) throws SQLException {
        final String sql = baseSelect() + " WHERE document_type = ? AND document = ?";
        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, type != null ? type.name() : null);
            ps.setString(2, document);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public boolean existsByDocument(DocumentType type, String document) throws SQLException {
        final String sql = "SELECT 1 FROM company WHERE document_type = ? AND document = ? LIMIT 1";
        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, type != null ? type.name() : null);
            ps.setString(2, document);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Company> listAll() throws SQLException {
        final String sql = baseSelect() + " ORDER BY name";
        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            List<Company> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Company> searchByName(String name, int limit) throws SQLException {
        final String sql = baseSelect() + " WHERE LOWER(name) LIKE LOWER(?) ORDER BY name LIMIT ?";
        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + (name == null ? "" : name.trim()) + "%");
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Company> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public void updateBasics(Company company) throws SQLException {
        final String sql = """
            UPDATE company
               SET name = ?,
                   document_type = ?,
                   document = ?,
                   version = version + 1
            WHERE id = ? AND version = ?
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
                if (rs.next()) {
                    company.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                    company.setVersion(rs.getInt("version"));
                } else {
                    throw new SQLException("Conflito de versão ao atualizar Company id=" + company.getId());
                }
            }
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState()) || String.valueOf(ex.getMessage()).toLowerCase().contains("unique")) {
                throw new DuplicateKeyException("Documento já cadastrado para outra empresa.", ex);
            }
            throw ex;
        }
    }

    public void deleteById(long id) throws SQLException {
        final String sql = "DELETE FROM company WHERE id = ?";
        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Nenhuma empresa removida. ID=" + id);
        }
    }

    /* HELPERS */

    private static String baseSelect() {
        return """
               SELECT id, version, creation_date, update_date, name, document_type, document
               FROM company
               """;
    }

    private static Company map(ResultSet rs) throws SQLException {
        Company company = new Company();
        company.setId(rs.getLong("id"));
        company.setVersion(rs.getInt("version"));
        company.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        company.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        company.setName(rs.getString("name"));
        company.setDocumentType(DocumentType.fromString(rs.getString("document_type")));
        company.setDocument(rs.getString("document"));
        return company;
    }
}
