package com.example.application.classes.repository;

import com.example.application.classes.model.Attendance;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceRepository {

    private final DataSource dataSource;

    public AttendanceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    /**
     * Insere um atendimento garantindo que o pet pertence à empresa.
     */
    public Optional<Long> insert(long companyId, Attendance attendance) throws SQLException {
        final String sql = """
            INSERT INTO attendance (animal_id, created_by_user_id, appointment_at, description)
            SELECT p.id, ?, ?, ?
              FROM pet p
             WHERE p.id = ?
               AND p.company_id = ?
            RETURNING id, version, creation_date, update_date
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setLongOrNull(ps, 1, attendance.getCreatedByUserId());
            ps.setObject(2, attendance.getAppointmentAt());
            ps.setString(3, attendance.getDescription());
            ps.setLong(4, attendance.getAnimalId());
            ps.setLong(5, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                long id = rs.getLong("id");
                attendance.setId(id);
                attendance.setVersion(rs.getInt("version"));
                attendance.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
                attendance.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return Optional.of(id);
            }
        }
    }


    public Optional<Attendance> findById(long companyId, long id) throws SQLException {
        final String sql = baseSelect() + """
            WHERE a.id = ?
              AND p.company_id = ?
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Attendance> listByAnimal(long companyId, long animalId) throws SQLException {
        final String sql = baseSelect() + """
            WHERE a.animal_id = ?
              AND p.company_id = ?
            ORDER BY a.appointment_at DESC NULLS LAST, a.id DESC
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, animalId);
            ps.setLong(2, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Attendance> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    /**
     * Atualiza dados básicos do atendimento com optimistic locking
     * e escopo por empresa.
     */
    public boolean updateBasics(long companyId, Attendance attendance) throws SQLException {
        final String sql = """
            UPDATE attendance a
               SET appointment_at = ?,
                   description    = ?,
                   update_date    = NOW(),
                   version        = a.version + 1
              FROM pet p
             WHERE a.id = ?
               AND a.version = ?
               AND a.animal_id = p.id
               AND p.company_id = ?
            RETURNING a.version, a.update_date
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setObject(1, attendance.getAppointmentAt());
            ps.setString(2, attendance.getDescription());
            ps.setLong(3, attendance.getId());
            ps.setInt(4, attendance.getVersion());
            ps.setLong(5, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                attendance.setVersion(rs.getInt("version"));
                attendance.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
                return true;
            }
        }
    }

    public boolean deleteById(long companyId, long id) throws SQLException {
        final String sql = """
            DELETE FROM attendance a
            USING pet p
            WHERE a.id = ?
              AND a.animal_id = p.id
              AND p.company_id = ?
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, companyId);

            return ps.executeUpdate() == 1;
        }
    }

    private static String baseSelect() {
        return """
            SELECT a.id,
                   a.version,
                   a.creation_date,
                   a.update_date,
                   a.animal_id,
                   a.created_by_user_id,
                   a.appointment_at,
                   a.description
              FROM attendance a
              JOIN pet p ON p.id = a.animal_id
            """;
    }

    private static Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getLong("id"));
        a.setVersion(rs.getInt("version"));
        a.setCreationDate(rs.getTimestamp("creation_date").toLocalDateTime());
        a.setUpdateDate(rs.getTimestamp("update_date").toLocalDateTime());
        a.setAnimalId(rs.getLong("animal_id"));
        a.setCreatedByUserId(rs.getObject("created_by_user_id", Long.class));

        Timestamp appt = rs.getTimestamp("appointment_at");
        a.setAppointmentAt(appt != null ? appt.toLocalDateTime() : null);

        a.setDescription(rs.getString("description"));
        return a;
    }

    private static void setLongOrNull(PreparedStatement ps, int idx, Long value) throws SQLException {
        if (value != null) {
            ps.setLong(idx, value);
        } else {
            ps.setNull(idx, Types.BIGINT);
        }
    }
}