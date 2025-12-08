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
import org.springframework.beans.factory.annotation.Autowired;


@PageTitle("Esqueci minha senha")
@Route(value = "forgot", layout = MainLayout.class)
@Menu(title = "Esqueci minha senha", icon = "la la-key", order = 3)
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

    private final AppUserService appUserService;

    private final EmailField email = new EmailField("E-mail");
    private final Button enviarBtn = new Button("Nova senha");
    private final Button voltarLoginBtn = new Button("Voltar para o login");

    @Autowired
    public ForgotPasswordView(AppUserService appUserService) {
        this.appUserService = appUserService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        var header = new ViewToolbar("Esqueci minha senha");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        H1 title = new H1("Esqueci minha senha");
        Paragraph subtitle = new Paragraph(
                "Digite seu e-mail. Se existir uma conta associada, enviaremos uma "
                        + "nova senha para você entrar."
        );

        email.setPlaceholder("voce@exemplo.com");
        email.setClearButtonVisible(true);
        email.setRequiredIndicatorVisible(true);
        email.setErrorMessage("Informe um e-mail válido");
        email.setWidth("320px");

        email.addValueChangeListener( ev -> {
            if (!email.isInvalid()) {
                email.setInvalid(false);
                email.setErrorMessage(null);
            }
        });

        enviarBtn.addClickShortcut(Key.ENTER);
        enviarBtn.addClickListener(e -> onSubmit());
        enviarBtn.addThemeNames("primary");

        voltarLoginBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("home")));
        voltarLoginBtn.addThemeNames("tertiary");


        content.add(title, subtitle, email, enviarBtn, voltarLoginBtn);
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
            boolean sent = appUserService.forgotPassword(value);

            if (sent) {
                var ok = Notification.show("Você receberá uma nova senha em instantes.", 5000, Notification.Position.BOTTOM_CENTER);
                ok.addThemeNames("success");

                email.clear();
            } else {
                var notOk = Notification.show(" E-mail não cadastrado.", 5000, Notification.Position.MIDDLE);
                notOk.addThemeNames("error");
            }
        } catch (Exception ex) {
            var n = Notification.show("Erro ao processar solicitação: " + ex.getMessage(), 6000, Notification.Position.MIDDLE);
            n.addThemeNames("error");
        } finally {
            setLoading(false);
        }
    }

    private void setLoading(boolean loading) {
        enviarBtn.setEnabled(!loading);
        enviarBtn.setText(loading ? "Enviando..." : "Enviar senha");
    }

}
