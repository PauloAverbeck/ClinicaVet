package com.example.application.classes.service;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final ObjectFactory<CurrentUserHolder> holderFactory;

    public CurrentUserService(ObjectFactory<CurrentUserHolder> holderFactory) {
        this.holderFactory = holderFactory;
    }

    private CurrentUserHolder holder() {
        return holderFactory.getObject();
    }

    public boolean isLoggedIn() { return holder().isLoggedIn(); }

    public long requireUserId() {
        if (!holder().isLoggedIn()) {
            throw new IllegalStateException("Usuário não autenticado na sessão.");
        }
        return holder().getUserId();
    }

    public void onLogin(long userId, String email) {
        holder().set(userId, email);
    }

    public void logout() {
        holder().clear();
    }
}