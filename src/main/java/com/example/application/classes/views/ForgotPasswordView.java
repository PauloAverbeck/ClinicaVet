package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.AppUserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.Objects;

@PageTitle("Esqueci minha senha")
@Route(value = "forgot", layout = MainLayout.class)
@Menu(title = "Esqueci minha senha", icon = "la la-key", order = 3)
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

    private final AppUserService appUserService;

    private final EmailField email = new EmailField("E-mail");
    private final Button submitBtn = new Button("Enviar nova senha");
    private final Button backBtn = new Button("Voltar para o login");

    public ForgotPasswordView(AppUserService appUserService) {
        this.appUserService = Objects.requireNonNull(appUserService);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        add(new ViewToolbar("Esqueci minha senha"));

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        H1 title = new H1("Esqueci minha senha");
        Paragraph subtitle = new Paragraph(
                "Digite seu e-mail. Se existir uma conta associada, enviaremos uma nova senha para você entrar."
        );

        configureEmailField();
        configureButtons();

        content.add(title, subtitle, email, submitBtn, backBtn);
    }

    private void configureEmailField() {
        email.setPlaceholder("voce@exemplo.com");
        email.setClearButtonVisible(true);
        email.setRequiredIndicatorVisible(true);
        email.setErrorMessage("Informe um e-mail válido");
        email.setWidth("320px");

        email.setPattern("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

        email.addValueChangeListener(ev -> {
            if (email.isInvalid()) {
                email.setInvalid(false);
            }
        });
    }

    private void configureButtons() {
        submitBtn.addThemeNames("primary");
        submitBtn.addClickShortcut(Key.ENTER);
        submitBtn.addClickListener(e -> onSubmit());

        backBtn.addThemeNames("tertiary");
        backBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("home")));
    }

    private void onSubmit() {
        final String value = trim(email.getValue());
        email.setValue(value);

        if (value.isBlank() || !value.matches(email.getPattern())) {
            email.setInvalid(true);
            email.focus();
            return;
        }

        setLoading(true);
        try {
            appUserService.forgotPassword(value);

            var ok = Notification.show(
                    "Se existir uma conta associada a este e-mail, você receberá uma nova senha em instantes.",
                    5000,
                    Notification.Position.BOTTOM_CENTER
            );
            ok.addThemeNames("success");

            email.clear();
        } catch (Exception ex) {
            var n = Notification.show(
                    "Erro ao processar solicitação: " + ex.getMessage(),
                    6000,
                    Notification.Position.MIDDLE
            );
            n.addThemeNames("error");
        } finally {
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        submitBtn.setEnabled(!loading);
        submitBtn.setText(loading ? "Enviando..." : "Enviar nova senha");
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}