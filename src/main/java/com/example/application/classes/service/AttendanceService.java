package com.example.application.classes.service;

import com.example.application.classes.model.Attendance;
import com.example.application.classes.repository.AttendanceRepository;
import com.example.application.config.ServiceGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ServiceGuard serviceGuard;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             ServiceGuard serviceGuard) {
        this.attendanceRepository = attendanceRepository;
        this.serviceGuard = serviceGuard;
    }

    private void validate(Attendance a) {
        a.setDescription(trimToEmpty(a.getDescription()));

        if (a.getAnimalId() <= 0L) {
            throw new AttendanceValidationException("O pet do atendimento é obrigatório.");
        }

        if (a.getAppointmentAt() == null && a.getDescription().isEmpty()) {
            throw new AttendanceValidationException(
                    "Preencha ao menos a data do atendimento ou a descrição."
            );
        }

        if (a.getDescription().length() > 2000) {
            throw new AttendanceValidationException("A descrição excede 2000 caracteres.");
        }

        if (a.getAppointmentAt() != null &&
                a.getAppointmentAt().isAfter(LocalDateTime.now().plusYears(2))) {
            throw new AttendanceValidationException(
                    "A data do atendimento não pode ser superior a 2 anos no futuro."
            );
        }
    }

    private static String trimToEmpty(String v) {
        return v == null ? "" : v.trim();
    }

    @Transactional
    public long create(Attendance attendance) throws SQLException {
        validate(attendance);

        long userId = serviceGuard.requireUserId();
        long companyId = serviceGuard.requireCompanyId();

        attendance.setCreatedByUserId(userId);

        return attendanceRepository.insert(companyId, attendance)
                .orElseThrow(() -> new AttendanceValidationException(
                        "Pet inválido ou não pertence à empresa selecionada."
                ));
    }


    @Transactional(readOnly = true)
    public Optional<Attendance> findById(long id) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        return attendanceRepository.findById(companyId, id);
    }

    @Transactional(readOnly = true)
    public List<Attendance> listByAnimalId(long animalId) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        return attendanceRepository.listByAnimal(companyId, animalId);
    }

    @Transactional
    public void updateBasics(Attendance attendance) throws SQLException {
        validate(attendance);

        long companyId = serviceGuard.requireCompanyId();

        boolean updated = attendanceRepository.updateBasics(companyId, attendance);
        if (!updated) {
            throw new AttendanceValidationException(
                    "Atendimento não encontrado, não pertence à empresa ou houve conflito de versão."
            );
        }
    }

    @Transactional
    public void deleteById(long id) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();

        boolean deleted = attendanceRepository.deleteById(companyId, id);
        if (!deleted) {
            throw new AttendanceValidationException(
                    "Atendimento não encontrado ou não pertence à empresa selecionada."
            );
        }
    }
}