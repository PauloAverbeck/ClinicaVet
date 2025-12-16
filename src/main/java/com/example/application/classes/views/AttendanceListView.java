package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Attendance;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.AttendanceService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.PetService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Atendimentos")
@Route(value = "pets/:id/attendances", layout = MainLayout.class)
public class AttendanceListView extends Main implements BeforeEnterObserver {

    private final PetService petService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<Attendance> grid = new Grid<>(Attendance.class, false);

    private final Button newBtn = new Button("Novo Atendimento");
    private final Button editBtn = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");

    private final ViewToolbar header = new ViewToolbar("Atendimentos");

    private Long petId;
    private Pet pet;

    private static final DateTimeFormatter ATT_FMT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yy");

    private static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "" : ATT_FMT.format(dt);
    }

    public AttendanceListView(
            PetService petService,
            AttendanceService attendanceService,
            CurrentUserService currentUserService,
            CurrentCompanyService currentCompanyService
    ) {
        this.petService = petService;
        this.attendanceService = attendanceService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        add(header);

        configureGrid();
        add(grid);

        buildActionsBar();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUserService.isLoggedIn()) {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("home");
            return;
        }

        if (!currentCompanyService.hasSelection()) {
            Notification.show("Selecione uma empresa para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("company/select");
            return;
        }

        var idParam = event.getRouteParameters().get("id").orElse(null);
        if (idParam == null || idParam.isBlank()) {
            Notification.show("Pet inválido.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("pets");
            return;
        }

        try {
            petId = Long.valueOf(idParam);
        } catch (NumberFormatException ex) {
            Notification.show("Pet inválido.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("pets");
            return;
        }

        try {
            pet = petService.findById(petId).orElse(null);
            if (pet == null) {
                Notification.show("Pet não encontrado.", 3000, Notification.Position.MIDDLE);
                event.rerouteTo("pets");
                return;
            }

            header.setTitle("Atendimentos • " + pet.getName());

        } catch (Exception ex) {
            Notification.show("Erro ao carregar pet: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            event.rerouteTo("pets");
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        reloadGrid();
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(Attendance::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(a -> formatDateTime(a.getAppointmentAt()))
                .setHeader("Atendimento em")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Attendance::getDescription)
                .setHeader("Descrição")
                .setFlexGrow(1);
    }

    private void buildActionsBar() {
        newBtn.addThemeNames("primary");
        editBtn.addThemeNames("primary");
        deleteBtn.addThemeNames("error");

        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        grid.asSingleSelect().addValueChangeListener(e -> {
            boolean hasSelection = e.getValue() != null;
            editBtn.setEnabled(hasSelection);
            deleteBtn.setEnabled(hasSelection);
        });

        newBtn.addClickListener(e -> onNew());
        editBtn.addClickListener(e -> onEdit());
        deleteBtn.addClickListener(e -> onDelete());

        HorizontalLayout actions = new HorizontalLayout(newBtn, editBtn, deleteBtn);
        actions.setPadding(true);
        add(actions);
    }

    private void reloadGrid() {
        if (petId == null) {
            grid.setItems(List.of());
            return;
        }
        try {
            List<Attendance> list = attendanceService.listByAnimalId(petId);
            grid.setItems(list);
        } catch (SQLException ex) {
            Notification.show("Erro ao carregar lista de atendimentos: " + ex.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            grid.setItems(List.of());
        }
    }

    private void onNew() {
        UI.getCurrent().navigate("attendance/new?pet=" + petId);
    }

    private void onEdit() {
        Attendance selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um atendimento para editar.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }
        UI.getCurrent().navigate("attendance/" + selected.getId() + "/edit");
    }

    private void onDelete() {
        Attendance selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione um atendimento para remover.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("warning");
            return;
        }

        try {
            attendanceService.deleteById(selected.getId());
            Notification.show("Atendimento removido com sucesso.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");
            reloadGrid();
            grid.asSingleSelect().clear();
            editBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        } catch (SQLException ex) {
            Notification.show("Erro ao remover atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }
}