package com.example.application.classes.repository;

import com.example.application.classes.model.Pet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PetRepository {

    private final DataSource dataSource;

    @Autowired
    public PetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* CREATE */

    public long insert(Pet pet) throws SQLException {
        final String sql = """
                INSERT INTO pet (company_id, client_id, created_by_user_id, name, species, breed, birth_date, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, version, creation_date, update_date
                """;

        try (Connection con = dataSource.getConnection();
        PreparedStatement preparedStatement = con.prepareStatement(sql)) {

            preparedStatement.setLong(1, pet.getCompanyId());
            preparedStatement.setLong(2, pet.getClientId());
            preparedStatement.setLong(3, pet.getCreatedByUserId());
            preparedStatement.setString(4, pet.getName());
            preparedStatement.setString(5, pet.getSpecies());
            preparedStatement.setString(6, pet.getBreed());
            preparedStatement.setObject(7, pet.getBirthDate());
            preparedStatement.setString(8, pet.getNotes());

            try (var rs = preparedStatement.executeQuery()) {
                rs.next();
                long id = rs.getLong("id");
                pet.setId(id);
                pet.setVersion(rs.getInt("version"));
                pet.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                pet.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return id;
            }
        }
    }

    /* READ */

    public Optional<Pet> findById(long companyId, long id) throws SQLException {
        final String sql = baseSelect() +
                " WHERE company_id = ? AND id = ? AND deleted_at IS NULL";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, id);

            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<Pet> listByClient(long companyId, long clientId) throws SQLException {
        final String sql = baseSelect() +
                " WHERE company_id = ? AND client_id = ? AND deleted_at IS NULL ORDER BY name, id";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, clientId);

            try (ResultSet resultSet = ps.executeQuery()) {
                List<Pet> pets = new ArrayList<>();
                while (resultSet.next()) {
                    pets.add(map(resultSet));
                }
                return pets;
            }
        }
    }

    public List<Pet> listByCompany(long companyId) throws SQLException {
        final String sql = baseSelect() +
                " WHERE company_id = ? AND deleted_at IS NULL ORDER BY id";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, companyId);

            try (ResultSet resultSet = ps.executeQuery()) {
                List<Pet> pets = new ArrayList<>();
                while (resultSet.next()) {
                    pets.add(map(resultSet));
                }
                return pets;
            }
        }
    }

    public List<Pet> searchByName(long companyId, long clientId, String namePattern) throws SQLException {
        final String sql = baseSelect() +
                " WHERE company_id = ? AND client_id = ? AND name ILIKE ? AND deleted_at IS NULL";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, companyId);
            ps.setLong(2, clientId);
            ps.setString(3, "%" + namePattern + "%");

            try (ResultSet resultSet = ps.executeQuery()) {
                List<Pet> pets = new ArrayList<>();
                while (resultSet.next()) {
                    pets.add(map(resultSet));
                }
                return pets;
            }
        }
    }

    /* UPDATE */

    public void updateBasics(Pet pet) throws SQLException {
        final String sql = """
                UPDATE pet
                SET client_id  = ?,
                    name       = ?,
                    species    = ?,
                    breed      = ?,
                    birth_date = ?,
                    notes      = ?,
                    version    = version + 1,
                    update_date = NOW()
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
                if (rs.next()) {
                    pet.setVersion(rs.getInt("version"));
                    pet.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                } else {
                    throw new SQLException("Conflito de modificações para o Pet com id= " + pet.getId());
                }
            }
        }
    }

    /* DELETE (soft) */

    public void softDelete(long companyId, long id) throws SQLException {
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
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Pet não encontrado ou já deletado. id=" + id);
            }
        }
    }

    /* HELPERS */

    private static String baseSelect() {
        return """
                SELECT  id,
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
        pet.setCreatedByUserId(rs.getLong("created_by_user_id"));
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
