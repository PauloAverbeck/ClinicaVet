package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.AppUserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Criar conta")
@Route(value = "signup", layout = MainLayout.class)
@Menu(title = "Sign Up", icon = "la la-user-plus", order = 2)
public class SignUpView extends VerticalLayout {

    private final AppUserService appUserService;

    private final TextField nameField = new TextField("Nome");
    private final EmailField emailField = new EmailField("E-mail");
    private final Button createBtn = new Button("Criar conta");

    public SignUpView(AppUserService appUserService) {
        this.appUserService = appUserService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSpacing(true);

        var header = new ViewToolbar("Sign Up");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        H1 title = new H1("Criar conta");

        nameField.setClearButtonVisible(true);
        nameField.setPlaceholder("Seu nome");
        nameField.setRequiredIndicatorVisible(true);

        emailField.setClearButtonVisible(true);
        emailField.setErrorMessage("Informe um e-mail válido");
        emailField.setPlaceholder("voce@exemplo.com");
        emailField.setRequiredIndicatorVisible(true);

        createBtn.addClickListener(e -> onCreate());
        createBtn.addClickShortcut(Key.ENTER);
        createBtn.setDisableOnClick(true);
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(title, nameField, emailField, createBtn);
    }

    private void onCreate() {
        String name  = trimOrNull(nameField.getValue());
        String email = trimOrNull(emailField.getValue());

        if (name == null || name.isBlank()) {
            Notification.show("Informe seu nome.", 3000, Notification.Position.MIDDLE);
            nameField.focus();
            return;
        }
        if (email == null || email.isBlank() || emailField.isInvalid()) {
            Notification.show("Informe um e-mail válido.", 3000, Notification.Position.MIDDLE);
            emailField.focus();
            return;
        }

        try {
            appUserService.requestSignup(name, email);

            Notification.show("Cadastro recebido! Enviamos uma senha provisória para seu e-mail.",4500, Notification.Position.MIDDLE);

            nameField.clear();
            emailField.clear();
            nameField.focus();
        } catch (IllegalStateException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Falha ao criar conta. Tente novamente.", 4000, Notification.Position.MIDDLE);
        }
    }

    private static String trimOrNull(String v) {
        return v == null ? null : v.trim();
    }
}