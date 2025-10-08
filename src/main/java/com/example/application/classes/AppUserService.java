package com.example.application.classes;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {
    private final AppUserDAO dao;
    public AppUserService(AppUserDAO dao) {
        this.dao = dao;
    }

    public long create(String name, String email, String passwordHash, LocalDateTime emailConfirmationTime, String provisionalPasswordHash) throws SQLException {
        String normalizeEmail = normalizeEmail(email);
        AppUser user = new AppUser()
                .setName(name)
                .setEmail(email)
                .setPasswordHash(passwordHash)
                .setEmailConfirmationTime(emailConfirmationTime)
                .setProvisionalPasswordHash(provisionalPasswordHash);
        try {
            return dao.insert(user);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(AppUser user) throws SQLException {
        user.setEmail(normalizeEmail(user.getEmail()));
        dao.update(user);
    }

    //SAVE ?
    //DELETE ?

    public Optional<AppUser> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public Optional<AppUser> findByEmail(String email) throws SQLException {
        return dao.findByEmail(normalizeEmail(email));
    }

    public List<AppUser> listAll() throws SQLException {
        return dao.listAll();
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
