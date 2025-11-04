package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Início")
@Route(value = "home", layout = MainLayout.class)
@Menu(title = "Início", icon = "la la-home", order = 0)
public class HomeView extends VerticalLayout {

    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Paragraph userInfo = new Paragraph();
    private final Paragraph companyInfo = new Paragraph();

    public HomeView(CurrentUserService currentUserService,
                    CurrentCompanyService currentCompanyService) {
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        setSizeFull();
        setSpacing(true);

        var header = new ViewToolbar("Início");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();
        content.setWidthFull();
        content.getStyle().set("display", "flex");
        content.getStyle().set("flex-direction", "column");
        content.getStyle().set("gap", "var(--lumo-space-m)");

        H1 title = new H1("Bem-vindo ao ClinicaVet");

        // Ações
        Button changeCompany = new Button("Trocar empresa", e -> UI.getCurrent().navigate("company/select"));
        changeCompany.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button logout = new Button("Sair", e -> {
            try {
                currentUserService.logout();
                currentCompanyService.clearSelection();
            } catch (Exception ignore) { }
            UI.getCurrent().navigate("login");
        });
        logout.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout actions = new HorizontalLayout(changeCompany, logout);

        content.add(title, userInfo, companyInfo, actions);

        refresh();
    }

    private void refresh() {
        if (!currentUserService.isLoggedIn()) {
            Notification.show("Sessão não autenticada. Faça login.", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("login");
            return;
        }

        long uid = currentUserService.requireUserId();
        userInfo.setText("Usuário logado: id=" + uid);

        if (currentCompanyService.hasSelection()) {
            String name = currentCompanyService.activeCompanyNameOrNull();
            boolean admin = currentCompanyService.isAdmin();
            companyInfo.setText("Empresa atual: " + name + (admin ? " (admin)" : ""));
        } else {
            companyInfo.setText("Nenhuma empresa selecionada.");
            Notification.show("Selecione uma empresa para continuar.", 3000, Notification.Position.BOTTOM_CENTER);
        }
    }
}