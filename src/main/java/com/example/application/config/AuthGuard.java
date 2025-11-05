package com.example.application.config;

import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.RouteScope;
import org.springframework.stereotype.Component;

@RouteScope
@Component
public class AuthGuard implements BeforeEnterObserver {

    private final CurrentUserService currentUserService;

    public AuthGuard(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // ignora rotas anotadas com @AnonymousAllowed
        var viewClass = event.getNavigationTarget();
        if (viewClass.isAnnotationPresent(AnonymousAllowed.class)) return;

        // se não está logado → redireciona
        if (!currentUserService.isLoggedIn()) {
            event.rerouteTo("home");
        }
    }
}