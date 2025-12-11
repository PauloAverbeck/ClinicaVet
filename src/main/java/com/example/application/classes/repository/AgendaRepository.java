package com.example.application.classes.repository;

import com.example.application.classes.service.AgendaRow;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AgendaRepository {

    private final DataSource dataSource;

    public AgendaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<AgendaRow> listByCompany(long companyId) throws SQLException {
        final String sql = """
            SELECT
                a.id,
                a.scheduled_at,
                a.appointment_at,
                p.name       AS pet_name,
                p.species    AS species,
                c.name       AS client_name,
                a.description
            FROM attendance a
            JOIN pet p    ON p.id = a.animal_id
            JOIN client c ON c.id = p.client_id
            WHERE p.company_id = ?
            ORDER BY COALESCE(a.appointment_at, a.scheduled_at) DESC
            """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, companyId);

            try (ResultSet rs = ps.executeQuery()) {
                List<AgendaRow> list = new ArrayList<>();
                while (rs.next()) {
                    var schedTs = rs.getTimestamp("scheduled_at");
                    var appTs   = rs.getTimestamp("appointment_at");

                    var scheduledAt   = schedTs != null ? schedTs.toLocalDateTime() : null;
                    var appointmentAt = appTs  != null ? appTs.toLocalDateTime()  : null;

                    var mainDateTime =
                            appointmentAt != null ? appointmentAt : scheduledAt;

                    boolean done = false;
                    if (mainDateTime != null) {
                        done = mainDateTime.isBefore(LocalDateTime.now());
                    }

                    list.add(new AgendaRow(
                            rs.getLong("id"),
                            mainDateTime,
                            done,
                            rs.getString("pet_name"),
                            rs.getString("species"),
                            rs.getString("client_name"),
                            rs.getString("description")
                    ));
                }
                return list;
            }
        }
    }
}
