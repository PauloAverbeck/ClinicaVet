package com.example.application.config;

import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;
import com.vaadin.flow.router.BeforeEnterEvent;

public final class ViewGuard {

    private ViewGuard() {}

    public static void requireLogin(BeforeEnterEvent event,
                                    CurrentUserService currentUserService) {
        if (currentUserService == null || !currentUserService.isLoggedIn()) {
            event.rerouteTo("home");
        }
    }

    public static void requireCompanySelected(BeforeEnterEvent event,
                                              CurrentCompanyService currentCompanyService) {
        if (currentCompanyService == null || !currentCompanyService.hasSelection()) {
            event.rerouteTo("company/select");
        }
    }

    public static void requireAdmin(BeforeEnterEvent event,
                                    CurrentUserService currentUserService,
                                    CurrentCompanyService currentCompanyService,
                                    UserCompanyService userCompanyService) {
        if (currentUserService == null || !currentUserService.isLoggedIn()) {
            event.rerouteTo("home");
            return;
        }
        if (currentCompanyService == null || !currentCompanyService.hasSelection()) {
            event.rerouteTo("company/select");
            return;
        }
        try {
            long userId = currentUserService.requireUserId();
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            if (!userCompanyService.isAdmin(userId, companyId)) {
                event.rerouteTo("home");
            }
        } catch (Exception e) {
            event.rerouteTo("home");
        }
    }
}