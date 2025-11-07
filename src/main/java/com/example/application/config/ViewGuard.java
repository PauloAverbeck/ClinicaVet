package com.example.application.config;

import com.example.application.classes.service.CurrentUserService;

public final class ViewGuard {
    private ViewGuard() {}
    public static void requireLogin(CurrentUserService currentUserService, Runnable onFail) {
        if (!currentUserService.isLoggedIn()) {
            onFail.run();
        }
    }
}
