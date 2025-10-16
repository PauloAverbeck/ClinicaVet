package com.example.application.classes.views;

import com.example.application.classes.AppUserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;


@PageTitle("Esqueci minha senha")
@Route("forgot")
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

    private final AppUserService appUserService;

    private final EmailField email = new EmailField("E-mail");
    private final Button enviarBtn = new Button("Enviar instruções");
    private final Button voltarLoginBtn = new Button("Voltar para o login");

    @Autowired
    public ForgotPasswordView(AppUserService appUserService) {
        this.appUserService = appUserService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        setSpacing(true);

        final var title = new H1("Esqueci minha senha");
        final var subtitle = new Paragraph("Informe o seu e-mail. Se houver uma conta associada, você receberá um e-mail com instruções para redefinir sua senha.");

        email.setClearButtonVisible(true);
        email.setWidth("400px");
        email.setErrorMessage("Informe um e-mail válido");
        email.setRequiredIndicatorVisible(true);

        email.addValueChangeListener( ev -> {
            if (!email.isInvalid()) {
                email.setInvalid(false);
                email.setErrorMessage(null);
            }
        });

        enviarBtn.addClickShortcut(Key.ENTER);
        enviarBtn.addClickListener(e -> onSubmit());
        enviarBtn.getElement().setProperty("title", "Enviar link de redefinição de senha");

        voltarLoginBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("login")));
        voltarLoginBtn.getElement().setProperty("title", "Voltar para a tela de login");

        final var form = new FormLayout();
        form.setMaxWidth("480px");
        form.add(email);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );

        final var actions = new VerticalLayout(enviarBtn, voltarLoginBtn);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWidth(400, Unit.PIXELS);

        add(title, subtitle, form, actions);
    }

    private void onSubmit() {
        final String value = email.getValue() != null ? email.getValue().trim() : "";
        email.setValue(value);

        if (value.isBlank() || email.isInvalid()) {
            email.setInvalid(true);
            email.setErrorMessage("E-mail é obrigatório e deve ser válido.");
            email.focus();
            return;
        }
        setLoading(true);
        try {
            String token = appUserService.requestPasswordReset(email.getValue());

            //TODO: remover exibição do token na notificação e implementar envio de e-mail
            var n1 = Notification.show("Token gerado: " + token, 8000, Notification.Position.MIDDLE);
            n1.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            var n2 = Notification.show("Acesse /reset?token=" + token, 8000, Notification.Position.BOTTOM_CENTER);
            n2.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            email.clear();
        } catch (Exception ex) {
            var n = Notification.show("Erro ao processar solicitação: " + ex.getMessage(), 6000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        enviarBtn.setEnabled(!loading);
        enviarBtn.setText(loading ? "Enviando..." : "Enviar instruções");
    }

}
