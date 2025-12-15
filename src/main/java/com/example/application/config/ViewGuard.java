package com.example.application.config;

import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;

public final class ViewGuard {

    private ViewGuard() {}

    public static void requireLogin(CurrentUserService currentUserService, Runnable onFail) {
        if (currentUserService == null || !currentUserService.isLoggedIn())  {
            onFail.run();
        }
    }

    public static void requireCompanySelected(CurrentCompanyService currentCompanyService, Runnable onFail) {
        if (currentCompanyService == null || !currentCompanyService.hasSelection()) {
            onFail.run();
        }
    }

    public static void requireAdmin(CurrentUserService currentUserService, CurrentCompanyService currentCompanyService,
                                    UserCompanyService userCompanyService, Runnable onFail) {
        if (!currentUserService.isLoggedIn() || !currentCompanyService.hasSelection()) {
            onFail.run();
            return;
        }
        try {
            long userId = currentUserService.requireUserId();
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            boolean admin = userCompanyService.isAdmin(userId, companyId);
            if (!admin) onFail.run();
        } catch (Exception ex) {
            onFail.run();
        }
    }
}

