package com.example.application.classes.service;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

@Component
@VaadinSessionScope
public class CurrentUserHolder {
    private Long userId;
    private String email;

    public boolean isLoggedIn() { return userId != null; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }

    public void set(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public void clear() {
        this.userId = null;
        this.email = null;
    }
}