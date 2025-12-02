package com.example.application.classes.service;

import com.example.application.classes.model.Attendance;
import com.example.application.classes.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CurrentUserService currentUserService;

    public AttendanceService(AttendanceRepository attendanceRepository, CurrentUserService currentUserService) {
        this.attendanceRepository = attendanceRepository;
        this.currentUserService = currentUserService;
    }

    /* CREATE */

    @Transactional
    public long create(Attendance attendance) throws SQLException {
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
        attendanceRepository.updateBasics(attendance);
    }

    /* DELETE */

    @Transactional
    public void deleteById(long id) throws SQLException {
        attendanceRepository.deleteById(id);
    }
}
