package com.example.application.classes.service;

import com.example.application.classes.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class CurrentUserService {
    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public long currentUserIdOrThrow() throws SQLException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Usuário não autenticado.");
        }
        String email = authentication.getName();
        return appUserRepository.findByEmail(email.toLowerCase().trim()).orElseThrow(() -> new IllegalStateException("Usuário não encontrado: " + email)).getId();
    }
}
