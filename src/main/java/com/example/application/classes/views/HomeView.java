package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.AppUserService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@PageTitle("Início")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@Menu(title = "Início", icon = "la la-home", order = 0)
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(HomeView.class);

    private final AppUserService appUserService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final EmailField email = new EmailField("E-mail");
    private final PasswordField password = new PasswordField("Senha");
    private final Button loginBtn = new Button("Entrar");

    private final Paragraph loggedInfo = new Paragraph();
    private final Button goSystemBtn = new Button("Ir para o sistema");
    private final Button selectCompanyBtn = new Button("Selecionar empresa");

    private final Anchor signUp = new Anchor("signup", "Criar conta");
    private final Anchor forgot = new Anchor("forgot", "Esqueci minha senha");

    public HomeView(AppUserService appUserService,
                    CurrentUserService currentUserService,
                    CurrentCompanyService currentCompanyService) {
        this.appUserService = Objects.requireNonNull(appUserService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.currentCompanyService = Objects.requireNonNull(currentCompanyService);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);

        add(new ViewToolbar("Início"));

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        H1 title = new H1("Bem-vindo ao Clínica Vet");

        configureFields();
        configureButtons();
        configureLinks();

        content.add(
                title,
                email, password, loginBtn,
                new Hr(),
                signUp,
                forgot,
                new Hr(),
                loggedInfo,
                goSystemBtn,
                selectCompanyBtn
        );

        updateUIState();
    }

    private void configureFields() {
        email.setPlaceholder("voce@exemplo.com");
        email.setClearButtonVisible(true);
        email.setRequiredIndicatorVisible(true);
        email.setErrorMessage("Informe um e-mail válido");

        email.setPattern("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

        email.addValueChangeListener(ev -> {
            if (email.isInvalid()) email.setInvalid(false);
        });

        password.setRevealButtonVisible(true);
    }

    private void configureButtons() {
        loginBtn.addThemeNames("primary");
        loginBtn.addClickShortcut(Key.ENTER);
        loginBtn.addClickListener(e -> onLogin());

        goSystemBtn.addThemeNames("primary");
        goSystemBtn.addClickListener(e -> UI.getCurrent().navigate("users"));

        selectCompanyBtn.addThemeNames("tertiary");
        selectCompanyBtn.addClickListener(e -> UI.getCurrent().navigate("company/select"));
    }

    private void configureLinks() {
        signUp.getElement().setProperty("title", "Ir para cadastro");
        forgot.getElement().setProperty("title", "Recuperar acesso");
    }

    private void updateUIState() {
        boolean loggedIn = currentUserService.isLoggedIn();

        email.setVisible(!loggedIn);
        password.setVisible(!loggedIn);
        loginBtn.setVisible(!loggedIn);
        signUp.setVisible(!loggedIn);
        forgot.setVisible(!loggedIn);

        loggedInfo.setVisible(loggedIn);
        goSystemBtn.setVisible(loggedIn);
        selectCompanyBtn.setVisible(loggedIn);

        if (loggedIn) {
            String company = currentCompanyService.activeCompanyNameOrNull();
            loggedInfo.setText(company == null || company.isBlank()
                    ? "Você já está logado. Nenhuma empresa selecionada."
                    : "Você já está logado. Empresa atual: " + company);
        }
    }

    private void onLogin() {
        final String e = trim(email.getValue());
        final String p = trim(password.getValue());

        if (e.isBlank() || !e.matches(email.getPattern())) {
            email.setInvalid(true);
            Notification.show("Informe um e-mail válido.", 3000, Notification.Position.MIDDLE);
            email.focus();
            return;
        }
        if (p.isBlank()) {
            Notification.show("Informe sua senha.", 3000, Notification.Position.MIDDLE);
            password.focus();
            return;
        }

        setLoading(true);
        try {
            var res = appUserService.loginOrConfirm(e, p);
            switch (res) {
                case LOGGED_IN -> {
                    var user = appUserService.findByEmail(e).orElseThrow();
                    currentUserService.onLogin(user.getId(), user.getEmail());

                    if (currentCompanyService.ensureAutoSelectionIfSingle(user.getId())) {
                        UI.getCurrent().navigate("users");
                    } else {
                        UI.getCurrent().navigate("company/select");
                    }
                }
                case INVALID -> Notification.show("E-mail ou senha inválidos.", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            log.error("Erro ao autenticar usuário", ex);
            Notification.show("Erro ao autenticar.", 3500, Notification.Position.MIDDLE);
        } finally {
            setLoading(false);
            updateUIState();
        }
    }

    private void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? "Entrando..." : "Entrar");
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
