package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.AppUserService;
import com.example.application.classes.AppUserService.LoginResult;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Entrar / Confirmar")
@Route(value = "login", layout = MainLayout.class)
@Menu(title = "Login", icon = "la la-sign-in-alt", order = 1)
public class LoginView extends VerticalLayout {
    private final AppUserService appUserService;

    private final EmailField emailField = new EmailField("E-mail");
    private final PasswordField passwordField = new PasswordField("Senha");
    private final Button loginBtn = new Button("Entrar / Confirmar");

    public LoginView(AppUserService appUserService) {
        this.appUserService = appUserService;
        ViewToolbar toolbar = new ViewToolbar("Login");
        add(toolbar);

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);

        H1 title = new H1("Entrar / Confirmar cadastro");

        emailField.setPlaceholder("voce@exemplo.com");
        emailField.setClearButtonVisible(true);
        emailField.setErrorMessage("Informe um e-mail válido");

        passwordField.setRevealButtonVisible(true);
        passwordField.setPlaceholder("Sua senha (pode ser a provisória)");

        loginBtn.addClickListener(e -> onLogin());

        add(title, emailField, passwordField, loginBtn);

        getElement().addEventListener("keyup", ev -> onLogin())
                .setFilter("event.key === 'Enter'");
    }

    private void onLogin() {
        String email = val(emailField.getValue());
        String pass  = val(passwordField.getValue());

        if (email == null || email.isBlank() || emailField.isInvalid()) {
            Notification.show("Informe um e-mail válido.", 3000, Notification.Position.MIDDLE);
            emailField.focus();
            return;
        }
        if (pass == null || pass.isBlank()) {
            Notification.show("Informe sua senha.", 3000, Notification.Position.MIDDLE);
            passwordField.focus();
            return;
        }

        try {
            LoginResult res = appUserService.loginOrConfirm(email, pass);
            switch (res) {
                case CONFIRMED -> {
                    Dialog d = new Dialog();
                    d.setHeaderTitle("E-mail confirmado!");
                    d.add(new Paragraph("Sua senha provisória foi promovida."));
                    d.add(new Paragraph("Você já pode continuar usando o sistema."));
                    Button ok = new Button("OK", ev -> d.close());
                    d.getFooter().add(ok);
                    d.open();
                    passwordField.clear();
                }
                case LOGGED_IN -> {
                    Notification.show("Login realizado com sucesso.", 3000, Notification.Position.MIDDLE);
                    UI.getCurrent().navigate("home");
                }
                case INVALID -> Notification.show("E-mail ou senha inválidos.", 3000, Notification.Position.MIDDLE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Falha ao autenticar. Tente novamente.", 4000, Notification.Position.MIDDLE);
        }
    }

    Anchor forgotPasswordLink() {
        Anchor a = new Anchor("forgot", "Esqueci minha senha");
        a.getElement().setProperty("title", "Ir para a tela de recuperação de senha");
        return a;
    }

    private static String val(String v) { return v == null ? null : v.trim(); }
}