package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.CompanyUserRow;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;

@PageTitle("Usuários da Empresa")
@Route(value = "company/users", layout = MainLayout.class)
@Menu(title = "Usuários da Empresa", icon = "la la-users", order = 7)
public class CompanyUsersView extends Main  {
    private CurrentUserService currentUserService;
    private CurrentCompanyService currentCompanyService;
    private UserCompanyService userCompanyService;

    private final Grid<CompanyUserRow> grid;

    public CompanyUsersView(
            CurrentUserService currentUserService,
            CurrentCompanyService currentCompanyService,
            UserCompanyService userCompanyService
    ) {

        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;
        this.userCompanyService = userCompanyService;

        var header = new ViewToolbar("Usuários da Empresa");
        add(header);

        this.grid = buildGrid();
        add(this.grid);
    }

    private Grid<CompanyUserRow> buildGrid() {
        final var grid = new Grid<CompanyUserRow>(CompanyUserRow.class, false);
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(CompanyUserRow::getUserId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(CompanyUserRow::getName)
                .setHeader("Nome")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(CompanyUserRow::getEmail)
                .setHeader("E-mail")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(row -> row.isAdmin() ? "Sim" : "Não")
                .setHeader("Admin")
                .setAutoWidth(true)
                .setSortable(true);

        return grid;
    }

    private void reloadGrid() {
        try {
            List<CompanyUserRow> companyUsers = userCompanyService.listCompanyUsers(currentCompanyService.activeCompanyIdOrThrow());
            grid.setItems(companyUsers);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao listar usuários: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            grid.setItems(List.of());
        }
    }

    // TODO Remover quando implementar controle de acesso “de verdade”
    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        try {
            ViewGuard.requireLogin(currentUserService, () -> {
                Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate("home");
            });

            long companyId = currentCompanyService.activeCompanyIdOrThrow();

            reloadGrid();
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar usuários da empresa: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("company/select");
        }
    }
}
