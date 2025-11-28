package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Client;
import com.example.application.classes.service.ClientService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;

@PageTitle("Clientes")
@Route(value = "clients", layout = MainLayout.class)
@Menu(title = "Clientes", icon = "la la-users", order = 8)
// TODO: usar @RolesAllowed quando implementar controle de acesso
public class ClientListView extends Main  {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<Client> grid;

    private final Button newBtn = new Button("Novo Cliente");
    private final Button editBtn = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");

    public ClientListView(ClientService clientService, CurrentUserService currentUserService, CurrentCompanyService currentCompanyService) {
        this.clientService = clientService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        var header = new ViewToolbar("Clientes");
        add(header);

        this.grid = buildGrid();
        add(grid);

        actionsBar();
        var actionsLayout = new HorizontalLayout(newBtn, editBtn, deleteBtn);
        actionsLayout.setPadding(true);
        add(actionsLayout);
    }

    private Grid<Client> buildGrid() {
        final var grid = new Grid<>(Client.class, false);
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(Client::getId)
            .setHeader("ID")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(Client::getName)
            .setHeader("Nome")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(Client::getEmail)
            .setHeader("Email")
            .setAutoWidth(true)
            .setSortable(true);

        grid.addColumn(Client::getPhone)
            .setHeader("Telefone")
            .setAutoWidth(true)
            .setSortable(true);

        return grid;
    }

    private void actionsBar() {
        newBtn.addThemeNames("success");
        editBtn.addThemeNames("primary");
        deleteBtn.addThemeNames("error");

        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        grid.asSingleSelect().addValueChangeListener(e -> {
            boolean hasSelection = e.getValue() != null;
            editBtn.setEnabled(hasSelection);
            deleteBtn.setEnabled(hasSelection);
        });

        newBtn.addClickListener(e ->
                onNew());

        editBtn.addClickListener(e ->
                onEditSelected());

        deleteBtn.addClickListener(e ->
                onDeleteSelected());
    }

    private void reloadGrid() {
        try {
            List<Client> clients = clientService.listAllForCompany();
            grid.setItems(clients);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar clientes: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            grid.setItems(List.of());
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        try {
            ViewGuard.requireLogin(currentUserService, () -> {
                Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
                UI.getCurrent().navigate("home");
            });

            currentCompanyService.activeCompanyIdOrThrow();

            reloadGrid();
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            Notification.show("Selecione uma empresa para ver os clientes.", 4000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("company/select");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar clientes: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("home");
        }
    }

    private void onNew() {
        UI.getCurrent().navigate("clients/new");
    }

    private void onEditSelected() {
        Client selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um cliente para editar.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }
        UI.getCurrent().navigate("clients/" + selected.getId() + "/edit");
    }

    private void onDeleteSelected() {
        Client selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um cliente para remover.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }

        try {
            clientService.softDelete(selected.getId());
            Notification.show("Cliente removido com sucesso.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");
            reloadGrid();
            grid.asSingleSelect().clear();
            deleteBtn.setEnabled(false);
            editBtn.setEnabled(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao remover cliente: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }
}
