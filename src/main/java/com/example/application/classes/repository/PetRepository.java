package com.example.application.classes.repository;

import com.example.application.classes.model.Pet;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PetRepository {

    private final DataSource dataSource;

    public PetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Long> insert(Pet pet) throws SQLException {
        final String sql = """
            INSERT INTO pet (
                company_id,
                client_id,
                created_by_user_id,
                name,
                species,
                breed,
                birth_date,
                notes
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, version, creation_date, update_date
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, pet.getCompanyId());
            ps.setLong(2, pet.getClientId());

            if (pet.getCreatedByUserId() != null) {
                ps.setLong(3, pet.getCreatedByUserId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }

            ps.setString(4, pet.getName());
            ps.setString(5, pet.getSpecies());
            ps.setString(6, pet.getBreed());
            ps.setObject(7, pet.getBirthDate());
            ps.setString(8, pet.getNotes());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                long id = rs.getLong("id");
                pet.setId(id);
                pet.setVersion(rs.getInt("version"));
                pet.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                pet.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());

                return Optional.of(id);
            }
        }
    }

    public Optional<Pet> findById(long companyId, long id) throws SQLException {
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

    public List<Pet> listByClient(long companyId, long clientId) throws SQLException {
        final String sql = baseSelect() + """
            WHERE company_id = ?
              AND client_id = ?
              AND deleted_at IS NULL
            ORDER BY name, id
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setLong(2, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Pet> pets = new ArrayList<>();
                while (rs.next()) pets.add(map(rs));
                return pets;
            }
        }
    }

    public List<Pet> listByCompany(long companyId) throws SQLException {
        final String sql = baseSelect() + """
            WHERE company_id = ?
              AND deleted_at IS NULL
            ORDER BY id
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Pet> pets = new ArrayList<>();
                while (rs.next()) pets.add(map(rs));
                return pets;
            }
        }
    }

    public List<Pet> searchByName(long companyId, long clientId, String nameQuery, int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, 200));

        final String sql = baseSelect() + """
            WHERE company_id = ?
              AND client_id = ?
              AND deleted_at IS NULL
              AND name ILIKE ?
            ORDER BY name
            LIMIT ?
            """;

        String like = "%" + (nameQuery == null ? "" : nameQuery.trim()) + "%";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);
            ps.setLong(2, clientId);
            ps.setString(3, like);
            ps.setInt(4, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                List<Pet> pets = new ArrayList<>();
                while (rs.next()) pets.add(map(rs));
                return pets;
            }
        }
    }

    public boolean updateBasics(Pet pet) throws SQLException {
        final String sql = """
            UPDATE pet
               SET client_id   = ?,
                   name        = ?,
                   species     = ?,
                   breed       = ?,
                   birth_date  = ?,
                   notes       = ?,
                   update_date = NOW(),
                   version     = version + 1
             WHERE company_id = ?
               AND id = ?
               AND version = ?
               AND deleted_at IS NULL
            RETURNING version, update_date
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, pet.getClientId());
            ps.setString(2, pet.getName());
            ps.setString(3, pet.getSpecies());
            ps.setString(4, pet.getBreed());
            ps.setObject(5, pet.getBirthDate());
            ps.setString(6, pet.getNotes());
            ps.setLong(7, pet.getCompanyId());
            ps.setLong(8, pet.getId());
            ps.setInt(9, pet.getVersion());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                pet.setVersion(rs.getInt("version"));
                pet.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return true;
            }
        }
    }

    public boolean softDelete(long companyId, long id) throws SQLException {
        final String sql = """
            UPDATE pet
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
            SELECT id,
                   version,
                   creation_date,
                   update_date,
                   company_id,
                   client_id,
                   created_by_user_id,
                   name,
                   species,
                   breed,
                   birth_date,
                   notes,
                   deleted_at
              FROM pet
            """;
    }

    private static Pet map(ResultSet rs) throws SQLException {
        Pet pet = new Pet();
        pet.setId(rs.getLong("id"));
        pet.setVersion(rs.getInt("version"));
        pet.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        pet.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());

        pet.setCompanyId(rs.getLong("company_id"));
        pet.setClientId(rs.getLong("client_id"));

        Long createdBy = rs.getObject("created_by_user_id", Long.class);
        pet.setCreatedByUserId(createdBy);

        pet.setName(rs.getString("name"));
        pet.setSpecies(rs.getString("species"));
        pet.setBreed(rs.getString("breed"));

        Date birthDate = rs.getDate("birth_date");
        pet.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);

        pet.setNotes(rs.getString("notes"));

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        pet.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);

        return pet;
    }
}