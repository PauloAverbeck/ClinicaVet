package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.AppUserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@PageTitle("Redefinir senha")
@Route(value = "reset", layout = MainLayout.class)
@Menu(title = "Reset Password", icon = "la la-key", order = 4)
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements HasUrlParameter<String> {
    private final AppUserService appUserService;
    private String token;

    private final PasswordField newPasswordField = new PasswordField("Nova senha");
    private final PasswordField confirmPasswordField = new PasswordField("Confirmar nova senha");
    private final Button saveBtn = new Button("Salvar nova senha");

    @Autowired
    public ResetPasswordView(AppUserService appUserService) {
        this.appUserService = appUserService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        setSpacing(true);

        var header = new ViewToolbar("Reset Password");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        content.add(
                new H1("Redefinir senha"),
                new Paragraph("Defina uma nova senha para sua conta.")
        );

        newPasswordField.setRevealButtonVisible(true);
        confirmPasswordField.setRevealButtonVisible(true);
        newPasswordField.setMinLength(8);
        newPasswordField.setErrorMessage("Mínimo de 8 caracteres");

        saveBtn.addClickShortcut(Key.ENTER);
        saveBtn.addClickListener(e -> onSubmit());

        content.add(newPasswordField, confirmPasswordField, saveBtn);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String ignored) {
        var params = event.getLocation().getQueryParameters().getParameters();
        this.token = params.getOrDefault("token", List.of("")).stream().findFirst().orElse("");
        if (token == null || token.isBlank()) {
            Notification.show("Token ausente ou inválido.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            getUI().ifPresent(ui -> ui.navigate("login"));
        }
    }

    private void onSubmit() {
        final String pass1 = Optional.ofNullable(newPasswordField.getValue()).map(String::trim).orElse("");
        final String pass2 = Optional.ofNullable(confirmPasswordField.getValue()).map(String::trim).orElse("");

        if (token == null || token.isBlank()) {
            Notification.show("Token ausente ou inválido.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (pass1.length() < 8) {
            newPasswordField.setInvalid(true);
            newPasswordField.setErrorMessage("A nova senha deve ter no mínimo 8 caracteres.");
            newPasswordField.focus();
            return;
        }
        if (!pass1.equals(pass2)) {
            confirmPasswordField.setInvalid(true);
            confirmPasswordField.setErrorMessage("As senhas não conferem.");
            confirmPasswordField.focus();
            return;
        }

        saveBtn.setEnabled(false);
        saveBtn.setDisableOnClick(true);
        saveBtn.setText("Salvando...");

        newPasswordField.addValueChangeListener(e -> validateForm());
        confirmPasswordField.addValueChangeListener(e -> validateForm());

        try {
            appUserService.resetPassword(token, pass1);
            Notification.show("Senha redefinida com sucesso. Você já pode entrar com a nova senha.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate("login"));
        } catch (Exception ex) {
            Notification.show("Erro ao redefinir senha: " + ex.getMessage(), 6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            saveBtn.setEnabled(true);
            saveBtn.setText("Salvar nova senha");
        }
    }

    private void validateForm() {
        String p1 = safe(newPasswordField.getValue());
        String p2 = safe(confirmPasswordField.getValue());

        boolean lengthOk = p1.length() >= 8;
        boolean up = p1.chars().anyMatch(Character::isUpperCase);
        boolean low = p1.chars().anyMatch(Character::isLowerCase);
        boolean digit = p1.chars().anyMatch(Character::isDigit);

        // Força da senha
        newPasswordField.setInvalid(!(lengthOk && up && low && digit));
        if (newPasswordField.isInvalid()) {
            newPasswordField.setErrorMessage("A senha deve ter no mínimo 8 caracteres, incluindo letras maiúsculas, minúsculas e números.");
        } else {
            newPasswordField.setErrorMessage(null);
        }

        // Confirmação
        boolean match = !p2.isBlank() && p1.equals(p2);
        confirmPasswordField.setInvalid(!match);
        if (confirmPasswordField.isInvalid()) {
            confirmPasswordField.setErrorMessage("As senhas não conferem.");
        } else {
            confirmPasswordField.setErrorMessage(null);
        }

        saveBtn.setEnabled(!newPasswordField.isInvalid() && !confirmPasswordField.isInvalid());
    }

    private static String safe(String s) {
        return Optional.ofNullable(s).map(String::trim).orElse("");
    }
}
