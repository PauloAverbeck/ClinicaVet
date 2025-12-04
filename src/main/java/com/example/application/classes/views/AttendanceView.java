package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Attendance;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.*;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;


@PageTitle("Atendimento")
@Route(value = "attendance/new", layout = MainLayout.class)
@RouteAlias(value = "attendance/:id/edit", layout = MainLayout.class)
public class AttendanceView extends Main implements BeforeEnterObserver {

    private final PetService petService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final DateTimePicker appointmentAtPicker = new DateTimePicker("Atendimento em");
    private final TextArea descriptionArea = new TextArea("Descrição");
    private final Button saveBtn = new Button("Salvar");
    private final ComboBox<Pet> petComboBox = new ComboBox<>("Pet");

    private Long attendanceId;
    private boolean isEditMode = false;

    public AttendanceView(PetService petService,
                          AttendanceService attendanceService,
                          CurrentUserService currentUserService,
                          CurrentCompanyService currentCompanyService) {
        this.petService = petService;
        this.attendanceService = attendanceService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        Locale locale = new Locale("pt", "BR");
        appointmentAtPicker.setLocale(locale);

        var header = new ViewToolbar("Atendimento");
        add(header);

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("600px");

        descriptionArea.setWidthFull();
        descriptionArea.setMinHeight("120px");
        descriptionArea.setMaxLength(2000);

        petComboBox.setRequiredIndicatorVisible(true);
        petComboBox.setHelperText("Obrigatório");
        petComboBox.setItemLabelGenerator(p -> p.getName() + " (ID: " + p.getId() + ")");

        form.add(petComboBox, appointmentAtPicker, descriptionArea);
        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        content.add(saveBtn);
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            Pet selectedPet = petComboBox.getValue();
            if (selectedPet == null) {
                Notification.show("Selecione um pet para o atendimento.", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("warning");
                return;
            }

            var appointmentAt = appointmentAtPicker.getValue();
            var description = descriptionArea.getValue() != null ? descriptionArea.getValue().trim() : "";

            if (isEditMode && attendanceId != null) {
                var attendance = attendanceService.findById(attendanceId)
                        .orElseThrow(() -> new IllegalStateException("Atendimento não encontrado para edição."));

                attendance.setAnimalId(selectedPet.getId());
                attendance.setAppointmentAt(appointmentAt);
                attendance.setDescription(description);

                attendanceService.updateBasics(attendance);

                Notification.show("Atendimento atualizado com sucesso.", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");
            } else {
                var attendance = new Attendance();
                attendance.setAnimalId(selectedPet.getId());
                attendance.setAppointmentAt(appointmentAt);
                attendance.setDescription(description);

                long id = attendanceService.create(attendance);

                Notification.show("Atendimento criado com sucesso. ID: " + id, 3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");
            }

            UI.getCurrent().navigate("pets/" + selectedPet.getId() + "/attendances");

        } catch (AttendanceValidationException vex) {
            Notification.show(vex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao salvar atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro inesperado ao salvar atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        ViewGuard.requireLogin(currentUserService, () -> {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            event.forwardTo("home");
        });

        try {
            currentCompanyService.activeCompanyIdOrThrow();
        } catch (Exception ex) {
            Notification.show("Selecione uma empresa para continuar.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            event.forwardTo("company/select");
            return;
        }

        try {
            loadPets();
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar pets: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            event.forwardTo("pets");
            return;
        }

        event.getRouteParameters().get("id").ifPresent(idStr -> {
            try {
                long id = Long.parseLong(idStr);
                this.attendanceId = id;
                this.isEditMode = true;
                loadExistingAttendance(id);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                Notification.show("ID de atendimento inválido: " + idStr, 5000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
                event.forwardTo("pets");
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Erro ao carregar atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
                event.forwardTo("pets");
            }
        });

        if (!isEditMode) {
            var params = event.getLocation().getQueryParameters().getParameters();
            var petList = params.get("pet");
            if (petList != null && !petList.isEmpty()) {
                try {
                    long petId = Long.parseLong(petList.getFirst());
                    petComboBox.getListDataView().getItems()
                            .filter(p -> p.getId() == petId)
                            .findFirst()
                            .ifPresent(petComboBox::setValue);
                } catch (NumberFormatException ignore) {
                }
            }
        }
    }

    private void loadExistingAttendance(long id) throws Exception {
        currentCompanyService.activeCompanyIdOrThrow();
        try {
            var opt = attendanceService.findById(id);
            if (opt.isEmpty()) {
                Notification.show("Atendimento não encontrado.", 5000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
                UI.getCurrent().navigate("attendances");
                return;
            }

            Attendance attendance = opt.get();

            if (attendance.getAppointmentAt() != null) {
                appointmentAtPicker.setValue(attendance.getAppointmentAt());
            } else {
                appointmentAtPicker.clear();
            }

            descriptionArea.setValue(
                    attendance.getDescription() != null ? attendance.getDescription() : ""
            );

            long animalId = attendance.getAnimalId();
            petComboBox.getListDataView().getItems()
                    .filter(p -> p.getId() == animalId)
                    .findFirst()
                    .ifPresent(petComboBox::setValue);

            isEditMode = true;
            this.attendanceId = id;
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar atendimento: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("attendances");
        }
    }

    private void loadPets() {
        try {
            var pets = petService.listAllForCompany();
            petComboBox.setItems(pets);

            if (pets.isEmpty()) {
                Notification.show(
                        "Nenhum pet encontrado. Cadastre um pet antes de criar um atendimento.",
                        5000,
                        Notification.Position.MIDDLE
                ).addThemeNames("warning");
            } else {
                saveBtn.setEnabled(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar pets: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            petComboBox.setItems(List.of());
            saveBtn.setEnabled(false);
        }
    }
}
