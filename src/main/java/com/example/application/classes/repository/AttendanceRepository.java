package com.example.application.classes.repository;

import com.example.application.classes.model.Attendance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceRepository {

    private final DataSource dataSource;

    @Autowired
    public AttendanceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* CREATE */

    public long insert(Attendance attendance) throws SQLException {
        final String sql = """
               INSERT INTO attendance
                    (animal_id, created_by_user_id, appointment_at, description)
               VALUES (?, ?, ?, ?)
               RETURNING id, version, creation_date, update_date
               """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, attendance.getAnimalId());

            if (attendance.getCreatedByUserId() != null) {
                ps.setLong(2, attendance.getCreatedByUserId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            ps.setObject(3, attendance.getAppointmentAt());
            ps.setString(4, attendance.getDescription());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong("id");
                attendance.setId(id);
                attendance.setVersion(rs.getInt("version"));
                attendance.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                attendance.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return id;
            }
        }
    }

    /* READ */

    public Optional<Attendance> findById(long id) throws SQLException {
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

    public List<Attendance> listByAnimal(long animalId) throws SQLException {
        final String sql = baseSelect()
                + " WHERE animal_id = ?"
                + " ORDER BY appointment_at DESC NULLS LAST, id DESC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, animalId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> attendances = new ArrayList<>();
                while (rs.next()) {
                    attendances.add(map(rs));
                }
                return attendances;
            }
        }
    }

    /* UPDATE */

    public void updateBasics(Attendance attendance) throws SQLException {
        final String sql = """
               UPDATE attendance
               SET appointment_at = ?, description = ?, update_date = NOW(), version = version + 1
               WHERE id = ? AND version = ?
               RETURNING version, update_date
               """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setObject(1, attendance.getAppointmentAt());
            ps.setString(2, attendance.getDescription());
            ps.setLong(3, attendance.getId());
            ps.setInt(4, attendance.getVersion());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    attendance.setVersion(rs.getInt("version"));
                    attendance.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                } else {
                    throw new SQLException("Conflito de versão ao atualizar atendimento com ID " + attendance.getId());
                }
            }
        }
    }

    /* DELETE (hard delete) */

    public void deleteById(long id) throws SQLException {
        final String sql = "DELETE FROM attendance WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Atendimento com ID " + id + " não encontrado para exclusão.");
            }
        }
    }

    /* HELPERS */

    private static String baseSelect() {
        return """
               SELECT id, version, creation_date, update_date,
                      animal_id, created_by_user_id, appointment_at, description
               FROM attendance
               """;
    }

    private static Attendance map(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setId(rs.getLong("id"));
        attendance.setVersion(rs.getInt("version"));
        attendance.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        attendance.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        attendance.setAnimalId(rs.getLong("animal_id"));

        Long createdBy = rs.getObject("created_by_user_id", Long.class);
        attendance.setCreatedByUserId(createdBy);

        Timestamp appointment = rs.getTimestamp("appointment_at");
        attendance.setAppointmentAt(appointment != null ? appointment.toLocalDateTime() : null);

        attendance.setDescription(rs.getString("description"));
        return attendance;
    }
}
