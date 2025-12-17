package com.example.application.classes.service;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final ObjectFactory<CurrentUserHolder> holderFactory;

    public CurrentUserService(ObjectFactory<CurrentUserHolder> holderFactory) {
        this.holderFactory = holderFactory;
    }

    private CurrentUserHolder holder() {
        return holderFactory.getObject();
    }

    public boolean isLoggedIn() {
        return holder().isLoggedIn();
    }

    public void requireLoggedIn() {
        if (!holder().isLoggedIn()) {
            throw new IllegalStateException("Usuário não autenticado na sessão.");
        }
    }

    public long requireUserId() {
        requireLoggedIn();
        long id = holder().getUserId();
        if (id <= 0) {
            throw new IllegalStateException("ID do usuário inválido na sessão.");
        }
        return id;
    }

    public String requireEmail() {
        requireLoggedIn();
        String email = holder().getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("E-mail do usuário não disponível na sessão.");
        }
        return email;
    }

    public Optional<Long> userIdOrEmpty() {
        return holder().isLoggedIn() ? Optional.of(holder().getUserId()) : Optional.empty();
    }

    public Optional<String> emailOrEmpty() {
        return holder().isLoggedIn() ? Optional.ofNullable(holder().getEmail()) : Optional.empty();
    }

    public void onLogin(long userId, String email) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId inválido.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email é obrigatório.");
        }
        holder().set(userId, email.trim());
    }

    public void logout() {
        holder().clear();
    }
}