package com.example.application.classes.service;

import com.example.application.classes.model.Attendance;
import com.example.application.classes.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CurrentUserService currentUserService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             CurrentUserService currentUserService) {
        this.attendanceRepository = attendanceRepository;
        this.currentUserService = currentUserService;
    }

    /* VALIDAÇÃO */

    private void validate(Attendance a) {
        // Normalização
        a.setDescription(trim(a.getDescription()));

        // Pet
        if (a.getAnimalId() == 0L) {
            throw new AttendanceValidationException("O pet do atendimento é obrigatório.");
        }

        // Regra mínima: precisa ter ao menos data ou descrição
        if (a.getAppointmentAt() == null && a.getDescription().isEmpty()) {
            throw new AttendanceValidationException(
                    "Preencha ao menos a data do atendimento ou a descrição."
            );
        }

        // Limite de tamanho da descrição
        if (a.getDescription().length() > 2000) {
            throw new AttendanceValidationException("A descrição excede 2000 caracteres.");
        }

        // Período válido para data do atendimento
        if (a.getAppointmentAt() != null &&
                a.getAppointmentAt().isBefore(LocalDateTime.now()) &&
                a.getAppointmentAt().isAfter(LocalDateTime.now().plusYears(2))) {
            throw new AttendanceValidationException("A data do atendimento deve ser entre agora e dois anos no futuro.");
        }
    }

    private String trim(String v) {
        return v == null ? "" : v.trim();
    }

    /* CREATE */

    @Transactional
    public long create(Attendance attendance) throws SQLException {
        validate(attendance);

        if (currentUserService.isLoggedIn()) {
            attendance.setCreatedByUserId(currentUserService.requireUserId());
        }
        return attendanceRepository.insert(attendance);
    }

    /* READ */

    @Transactional(readOnly = true)
    public Optional<Attendance> findById(long id) throws SQLException {
        return attendanceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Attendance> listByAnimalId(long animalId) throws SQLException {
        return attendanceRepository.listByAnimal(animalId);
    }

    /* UPDATE */

    @Transactional
    public void updateBasics(Attendance attendance) throws SQLException {
        validate(attendance);
        attendanceRepository.updateBasics(attendance);
    }

    /* DELETE */

    @Transactional
    public void deleteById(long id) throws SQLException {
        attendanceRepository.deleteById(id);
    }
}
