package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.AgendaRow;
import com.example.application.classes.service.AgendaService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@PageTitle("Agenda")
@Route(value = "agenda", layout = MainLayout.class)
@Menu(title = "Agenda", icon = "vaadin:calendar")
public class AgendaView extends Main {

    private final AgendaService agendaService;
    private final CurrentCompanyService currentCompanyService;
    private final CurrentUserService currentUserService;

    private final Grid<AgendaRow> grid = new Grid<>(AgendaRow.class, false);
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");
    private List<AgendaRow> allItems = Collections.emptyList();

    public AgendaView (AgendaService agendaService,
                       CurrentCompanyService currentCompanyService, CurrentUserService currentUserService) {
        this.agendaService = agendaService;
        this.currentCompanyService = currentCompanyService;
        this.currentUserService = currentUserService;

        var header = new ViewToolbar("Agenda");
        add(header);

        configureStatusFilter();
        configureGrid();
        add(statusFilter, grid);
        loadData();
    }

    private void configureGrid() {
        grid.setWidthFull();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(row -> row.mainDateTime() != null ? row.mainDateTime().format(formatter) : "")
                .setHeader("Data/Hora")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(row -> row.done() ? "Realizado" : "Agendado")
                .setHeader("Status")
                .setAutoWidth(true);

        grid.addColumn(AgendaRow::petName)
                .setHeader("Pet")
                .setAutoWidth(true);

        grid.addColumn(AgendaRow::species)
                .setHeader("Espécie")
                .setAutoWidth(true);

        grid.addColumn(AgendaRow::clientName)
                .setHeader("Tutor")
                .setAutoWidth(true);

        grid.addColumn(AgendaRow::description)
                .setHeader("Descrição")
                .setFlexGrow(1);
    }

    private void configureStatusFilter() {
        statusFilter.setItems("Todos", "Agendados", "Realizados");
        statusFilter.setValue("Todos");
        statusFilter.setClearButtonVisible(false);
        statusFilter.addValueChangeListener(e -> applyFilter());
    }

    private void applyFilter() {
        if (allItems.isEmpty()) {
            grid.setItems(Collections.emptyList());
            return;
        }

        String filter = statusFilter.getValue();
        if (filter == null || filter.equals("Todos")) {
            grid.setItems(allItems);
            return;
        }

        boolean doneFilter = filter.equals("Realizados");

        grid.setItems(allItems.stream()
                .filter(row -> row.done() == doneFilter)
                .toList()
        );
    }

    private void loadData() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();
            allItems = agendaService.listCurrentCompanyAgenda();
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Erro ao carregar dados da agenda: " + e.getMessage());
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        ViewGuard.requireLogin(currentUserService, () -> {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("home");
        });
        ViewGuard.requireCompanySelected(currentCompanyService, () -> {
            Notification.show("Selecione uma empresa para continuar.", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("company/select");
        });
    }
}
