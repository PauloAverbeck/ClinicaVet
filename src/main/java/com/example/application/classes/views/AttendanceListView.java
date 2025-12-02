package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Attendance;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.AttendanceService;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@PageTitle("Atendimentos")
@Route(value = "pets/:id/attendances", layout = MainLayout.class)
//TODO: usar @RolesAllowed quando implementar controle de acesso
public class AttendanceListView extends Main implements BeforeEnterObserver {

    private final PetService petService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<Attendance> grid = new Grid<>(Attendance.class, false);

    private Button newBtn = new Button("Novo Atendimento");
    private Button editBtn = new Button("Editar");
    private Button deleteBtn = new Button("Remover");
    private Long petId;
    private Pet pet;

    private static final DateTimeFormatter ATT_FMT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yy");
    private static String formateDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return ATT_FMT.format(dt);
    }

    public AttendanceListView(PetService petService, AttendanceService attendanceService, CurrentUserService currentUserService, CurrentCompanyService currentCompanyService) {
        this.petService = petService;
        this.attendanceService = attendanceService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        add(new ViewToolbar("Atendimentos"));

        configureGrid();
        add(grid);

        actionsBar();
    }

    private void actionsBar() {
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

    private void configureGrid() {
        grid.setWidthFull();

        grid.addColumn(Attendance::getId)
                .setHeader("ID")
                .setAutoWidth(true);

        grid.addColumn(a -> formateDateTime(a.getScheduledAt()))
                .setHeader("Agendado para")
                .setAutoWidth(true);

        grid.addColumn(a -> formateDateTime(a.getAppointmentAt()))
                .setHeader("Atendimento em")
                .setAutoWidth(true);

        grid.addColumn(Attendance::getDescription)
                .setHeader("Descrição")
                .setAutoWidth(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuard.requireLogin(currentUserService, () -> {
            Notification.show("Faça login para continuar.");
            event.forwardTo("home");
        });

        try {
            currentCompanyService.activeCompanyIdOrThrow();
        } catch (Exception ex) {
            Notification.show("Selecione uma empresa.");
            event.forwardTo("company/select");
            return;
        }

        Optional<String> idParam = event.getRouteParameters().get("id");
        if (idParam.isEmpty()) {
            Notification.show("Pet inválido.");
            event.forwardTo("pets");
            return;
        }

        try {
            petId = Long.valueOf(idParam.get());
            pet = petService.findById(petId).orElse(null);
            if (pet == null) {
                    Notification.show("Pet não encontrado.");
                    event.forwardTo("pets");
                    return;
            }
        } catch (Exception ex) {
            Notification.show("Erro ao carregar pet: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                .addThemeNames("error");
            event.forwardTo("pets");
            return;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        if (petId != null) {
            reloadGrid();
        }
    }

    private void reloadGrid() {
        try {
            List<Attendance> list = attendanceService.listByAnimalId(petId);
            grid.setItems(list);
        } catch (SQLException ex) {
            Notification.show("Erro ao carregar lista de atendimentos: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                .addThemeNames("error");
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
            ex.printStackTrace();
            Notification.show("Erro ao remover atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }
}
