package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Client;
import com.example.application.classes.service.ClientService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@PageTitle("Clientes")
@Route(value = "clients", layout = MainLayout.class)
@Menu(title = "Clientes", icon = "la la-users", order = 8)
public class ClientListView extends Main implements BeforeEnterObserver {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<Client> grid = new Grid<>(Client.class, false);

    private final Button newBtn = new Button("Novo Cliente");
    private final Button editBtn = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");

    public ClientListView(ClientService clientService,
                          CurrentUserService currentUserService,
                          CurrentCompanyService currentCompanyService) {
        this.clientService = Objects.requireNonNull(clientService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.currentCompanyService = Objects.requireNonNull(currentCompanyService);

        add(new ViewToolbar("Clientes"));

        configureGrid();
        configureActions();

        var actionsLayout = new HorizontalLayout(newBtn, editBtn, deleteBtn);
        actionsLayout.setPadding(true);

        add(grid, actionsLayout);
        setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuard.requireLogin(event, currentUserService);
        if (!currentUserService.isLoggedIn()) return;

        ViewGuard.requireCompanySelected(event, currentCompanyService);
        if (!currentCompanyService.hasSelection()) return;

        reloadGrid();
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(Client::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        grid.addColumn(Client::getName).setHeader("Nome").setAutoWidth(true).setSortable(true);
        grid.addColumn(Client::getEmail).setHeader("Email").setAutoWidth(true).setSortable(true);
        grid.addColumn(Client::getPhone).setHeader("Telefone").setAutoWidth(true).setSortable(true);
    }

    private void configureActions() {
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

        newBtn.addClickListener(e -> UI.getCurrent().navigate("clients/new"));
        editBtn.addClickListener(e -> onEditSelected());
        deleteBtn.addClickListener(e -> onDeleteSelected());
    }

    private void reloadGrid() {
        try {
            List<Client> clients = clientService.listAllForCompany();
            grid.setItems(clients);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar clientes: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE).addThemeNames("error");
            grid.setItems(List.of());
        }
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
            editBtn.setEnabled(false);
            deleteBtn.setEnabled(false);

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao remover cliente: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE).addThemeNames("error");
        }
    }
}