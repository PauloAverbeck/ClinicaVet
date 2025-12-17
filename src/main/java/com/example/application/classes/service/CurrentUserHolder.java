package com.example.application.classes.service;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

@Component
@VaadinSessionScope
public class CurrentUserHolder {

    private Long userId;
    private String email;

    public boolean isLoggedIn() {
        return userId != null && userId > 0;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void set(Long userId, String email) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId inválido para sessão.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email inválido para sessão.");
        }
        this.userId = userId;
        this.email = email.trim();
    }

    public void clear() {
        this.userId = null;
        this.email = null;
    }
}