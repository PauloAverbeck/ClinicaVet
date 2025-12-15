package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.PetService;
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

@PageTitle("Pets")
@Route(value = "pets", layout = MainLayout.class)
@Menu(title = "Pets", icon = "la la-paw", order = 10)
public class PetListView extends Main {

    private final PetService petService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<Pet> grid;

    private final Button newBtn = new Button("Novo Pet");
    private final Button editBtn = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");
    private final Button attendanceBtn = new Button("Atendimentos");

    public PetListView(PetService petService,
                       CurrentUserService currentUserService,
                       CurrentCompanyService currentCompanyService) {
        this.petService = petService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        var header = new ViewToolbar("Pets");
        add(header);

        this.grid = buildGrid();
        add(grid);

        actionsBar();
        var actionsLayout = new HorizontalLayout(newBtn, editBtn, deleteBtn, attendanceBtn);
        actionsLayout.setPadding(true);
        add(actionsLayout);
    }

    private Grid<Pet> buildGrid() {
        final var grid = new Grid<>(Pet.class, false);
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(Pet::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Pet::getName)
                .setHeader("Nome")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Pet::getSpecies)
                .setHeader("Espécie")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Pet::getBreed)
                .setHeader("Raça")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(p -> p.getBirthDate() != null ? p.getBirthDate() : null)
                .setHeader("Data de Nascimento")
                .setAutoWidth(true)
                .setSortable(true);

        return grid;
    }

    private void actionsBar() {
        newBtn.addThemeNames("success");
        editBtn.addThemeNames("primary");
        deleteBtn.addThemeNames("error");
        attendanceBtn.addThemeNames("tertiary");

        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        attendanceBtn.setEnabled(false);

        grid.asSingleSelect().addValueChangeListener(e -> {
            boolean hasSelection = e.getValue() != null;
            editBtn.setEnabled(hasSelection);
            deleteBtn.setEnabled(hasSelection);
            attendanceBtn.setEnabled(hasSelection);
        });

        newBtn.addClickListener(e -> onNew());
        editBtn.addClickListener(e -> onEditSelected());
        deleteBtn.addClickListener(e -> onDeleteSelected());
        attendanceBtn.addClickListener(e -> onAttendanceForSelected());
    }

    private void reloadGrid() {
        try {
            List<Pet> pets = petService.listAllForCompany();
            grid.setItems(pets);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar lista de pets: " + ex.getMessage(),
                            5000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
            grid.setItems(List.of());
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        try {
            ViewGuard.requireLogin(currentUserService, () -> {
                Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
                UI.getCurrent().navigate("home");
            });
            ViewGuard.requireCompanySelected(currentCompanyService, () -> {;
                Notification.show("Selecione uma empresa para ver os pets.", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("warning");
                UI.getCurrent().navigate("company/select");
            });

            currentCompanyService.activeCompanyIdOrThrow();
            reloadGrid();
        } catch (Exception e) {
            Notification.show("Erro ao carregar lista de pets: " + e.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("home");
        }
    }

    private void onNew() {
        UI.getCurrent().navigate("pets/new");
    }

    private void onEditSelected() {
        Pet selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um pet para editar.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }
        UI.getCurrent().navigate("pets/" + selected.getId() + "/edit");
    }

    private void onDeleteSelected() {
        Pet selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um pet para remover.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }

        try {
            petService.softDelete(selected.getId());
            Notification.show("Pet removido com sucesso.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");
            reloadGrid();
            grid.asSingleSelect().clear();
            editBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            attendanceBtn.setEnabled(false);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao remover pet: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    private void onAttendanceForSelected() {
        Pet selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um pet.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }
        UI.getCurrent().navigate("pets/" + selected.getId() + "/attendances");
    }
}
