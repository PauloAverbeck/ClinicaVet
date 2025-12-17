package com.example.application.config;

import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import org.springframework.stereotype.Component;

@Component
public class ServiceGuard {

    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    public ServiceGuard(CurrentUserService currentUserService,
                        CurrentCompanyService currentCompanyService) {
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;
    }

    public long requireUserId() {
        return currentUserService.requireUserId();
    }

    public long requireCompanyId() {
        currentUserService.requireUserId();
        return currentCompanyService.activeCompanyIdOrThrow();
    }

    public void requireAdmin() {
        requireCompanyId();
        if (!currentCompanyService.isAdmin()) {
            throw new SecurityException("Usuário não possui privilégios de administrador na empresa selecionada.");
        }
    }
}