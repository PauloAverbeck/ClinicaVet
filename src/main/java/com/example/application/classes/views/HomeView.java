package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.*;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;

@PageTitle("Início")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@Menu(title = "Início", icon = "la la-home", order = 0)
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    private final AppUserService appUserService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;
    private final UserCompanyService userCompanyService;

    private final EmailField email = new EmailField("E-mail");
    private final PasswordField password = new PasswordField("Senha");
    private final Button loginBtn = new Button("Entrar");
    private final Button exitBtn = new Button("Sair");

    public HomeView(AppUserService appUserService,
                    CurrentUserService currentUserService,
                    CurrentCompanyService currentCompanyService,
                    UserCompanyService userCompanyService) {
        this.appUserService = appUserService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;
        this.userCompanyService = userCompanyService;

        setSizeFull();
        var header = new ViewToolbar("Início");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();
        content.setWidth("420px");
        content.getStyle().set("display", "flex");
        content.getStyle().set("flexDirection", "column");
        content.getStyle().set("gap", "var(--lumo-space-m)");

        H1 title = new H1("Bem-vindo ao Clínica Vet");

        // Card de Login
        email.setPlaceholder("voce@exemplo.com");
        email.setClearButtonVisible(true);
        password.setRevealButtonVisible(true);

        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.addClickShortcut(Key.ENTER);
        loginBtn.addClickListener(e -> onLogin());

        exitBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        exitBtn.addClickListener(e -> onLogout());
        exitBtn.setVisible(false);

        var signUp = new Anchor("signup", "Criar conta");
        signUp.getElement().setProperty("title", "Ir para cadastro");
        var forgot = new Anchor("forgot", "Esqueci minha senha");
        forgot.getElement().setProperty("title", "Recuperar acesso");

        content.add(
                title,
                email, password, loginBtn, exitBtn,
                new Hr(),
                signUp,
                forgot
        );
        updateUIState();

        // Se já está logado e já tem empresa ativa, mostrar info resumida aqui
        if (currentUserService.isLoggedIn() && currentCompanyService.hasSelection()) {
            var info = new Paragraph("Usuário logado: " + userDisplay()
                    + " • Empresa atual: " + currentCompanyService.activeCompanyNameOrNull());
            content.add(new Hr(), info);
        }
    }

    private void updateUIState() {
        boolean loggedIn = currentUserService.isLoggedIn();
        email.setEnabled(!loggedIn);
        password.setEnabled(!loggedIn);
        loginBtn.setVisible(!loggedIn);
        exitBtn.setVisible(loggedIn);
    }

    private void onLogin() {
        final String e = val(email.getValue());
        final String p = val(password.getValue());
        if (e == null || e.isBlank() || email.isInvalid()) {
            Notification.show("Informe um e-mail válido.", 3000, Notification.Position.MIDDLE);
            email.focus(); return;
        }
        if (p == null || p.isBlank()) {
            Notification.show("Informe sua senha.", 3000, Notification.Position.MIDDLE);
            password.focus(); return;
        }
        try {
            var res = appUserService.loginOrConfirm(e, p);
            switch (res) {
                case CONFIRMED -> {
                    Notification.show("Senha provisória promovida. Faça login novamente.", 3000, Notification.Position.MIDDLE);
                    password.clear();
                    password.focus();
                }
                case LOGGED_IN -> {
                    // “Loga” na sessão
                    var user = appUserService.findByEmail(e).orElseThrow();
                    currentUserService.onLogin(user.getId(), user.getEmail());
                    // Seleção de empresa
                    if (currentCompanyService.ensureAutoSelectionIfSingle(user.getId())) {
                        UI.getCurrent().navigate("users");
                        return;
                    }
                    List<CompanyChoice> choices = userCompanyService.companyChoicesFor(user.getId());
                    if (choices.isEmpty()) {
                        UI.getCurrent().navigate("company/select");
                    } else {
                        UI.getCurrent().navigate("company/select");
                    }
                }
                case INVALID -> Notification.show("E-mail ou senha inválidos.", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Falha ao autenticar.", 3500, Notification.Position.MIDDLE);
        }
        updateUIState();
    }

    private void onLogout() {
        currentUserService.logout();
        currentCompanyService.clearSelection();
        Notification.show("Sessão encerrada.", 2500, Notification.Position.MIDDLE);
        UI.getCurrent().navigate("home");
        UI.getCurrent().getPage().reload();
    }

    private String userDisplay() {
        try {
            if (!currentUserService.isLoggedIn()) return "—";
            long id = currentUserService.requireUserId();
            return appUserService.findById(id).map(u -> u.getName() + " (id " + id + ")").orElse("id " + id);
        } catch (Exception e) {
            return "—";
        }
    }

    private static String val(String v) { return v == null ? null : v.trim(); }
}