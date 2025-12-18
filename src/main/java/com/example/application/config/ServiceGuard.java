package com.example.application.config;

import com.example.application.classes.repository.UserCompanyRepository;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class ServiceGuard {

    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;
    private final UserCompanyRepository userCompanyRepository;

    public ServiceGuard(CurrentUserService currentUserService,
                        CurrentCompanyService currentCompanyService, UserCompanyRepository userCompanyRepository) {
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;
        this.userCompanyRepository = userCompanyRepository;
    }

    public long requireUserId() {
        return currentUserService.requireUserId();
    }

    public long requireCompanyId() {
        requireUserId();
        return currentCompanyService.activeCompanyIdOrThrow();
    }

    public void requireAdmin() {
        requireCompanyId();
        if (!currentCompanyService.isAdmin()) {
            throw new SecurityException("Usuário não possui privilégios de administrador na empresa selecionada.");
        }
    }

    public void requireAdminOfCompany(long companyId) {
        long userId = requireUserId();
        try {
            boolean isAdmin = userCompanyRepository.isUserAdmin(userId, companyId);
            if (!isAdmin) {
                throw new SecurityException("Usuário não possui privilégios de administrador nesta empresa.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao validar admin para companyId=" + companyId, e);
        }
    }

    public void requireSelectedCompanyEquals(long companyId) {
        long activeId = requireCompanyId();
        if (activeId != companyId) {
            throw new SecurityException("Operação não permitida fora da empresa selecionada.");
        }
    }
}