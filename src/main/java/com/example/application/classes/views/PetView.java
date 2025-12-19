package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Client;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@PageTitle("Pet")
@Route(value = "pets/new", layout = MainLayout.class)
@RouteAlias(value = "pets/:id/edit", layout = MainLayout.class)
@Menu(title = "Cadastro de Pet", icon = "la la-paw-plus", order = 11)
public class PetView extends Main implements BeforeEnterObserver {

    private final PetService petService;
    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final ComboBox<Client> clientField = new ComboBox<>("Cliente");
    private final TextField nameField = new TextField("Nome");
    private final TextField speciesField = new TextField("Espécie");
    private final TextField breedField = new TextField("Raça");
    private final DatePicker birthDatePicker = new DatePicker("Data de Nascimento");
    private final TextArea notesArea = new TextArea("Notas");
    private final Button saveBtn = new Button("Salvar");
    private final Button returnBtn = new Button("Voltar");

    private Long petId = null;

    public PetView(PetService petService,
                   ClientService clientService,
                   CurrentUserService currentUserService,
                   CurrentCompanyService currentCompanyService) {

        this.petService = Objects.requireNonNull(petService);
        this.clientService = Objects.requireNonNull(clientService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.currentCompanyService = Objects.requireNonNull(currentCompanyService);

        add(new ViewToolbar("Pet"));

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("600px");

        configureFields();

        form.add(
                clientField,
                nameField,
                speciesField,
                breedField,
                birthDatePicker,
                notesArea
        );

        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        returnBtn.addThemeNames("tertiary");
        returnBtn.addClickListener(e -> UI.getCurrent().navigate("pets"));

        var buttons = new HorizontalLayout();
        buttons.add(saveBtn, returnBtn);
        content.add(buttons);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        if (!currentUserService.isLoggedIn()) {
            Notification.show("Faça login para continuar.", 3000, Position.MIDDLE);
            event.rerouteTo("home");
            return;
        }

        if (!currentCompanyService.hasSelection()) {
            Notification.show("Selecione uma empresa para continuar.", 3000, Position.MIDDLE);
            event.rerouteTo("company/select");
            return;
        }

        try {
            loadClients();
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar clientes: " + ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeNames("error");
            event.rerouteTo("pets");
            return;
        }

        petId = event.getRouteParameters().getLong("id").orElse(null);
        if (petId != null) {
            try {
                loadExistingPet(petId);
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Erro ao carregar pet: " + ex.getMessage(), 5000, Position.MIDDLE)
                        .addThemeNames("error");
                event.rerouteTo("pets");
            }
        }
    }

    private void configureFields() {
        clientField.setRequiredIndicatorVisible(true);
        clientField.setHelperText("Obrigatório");
        clientField.setItemLabelGenerator(Client::getName);

        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("Obrigatório");
        nameField.setMaxLength(200);

        speciesField.setRequiredIndicatorVisible(true);
        speciesField.setHelperText("Obrigatório");
        speciesField.setMaxLength(50);

        breedField.setRequiredIndicatorVisible(true);
        breedField.setHelperText("Obrigatório");
        breedField.setMaxLength(100);

        notesArea.setWidthFull();
        notesArea.setMaxLength(1000);
        notesArea.setHelperText("Observações gerais sobre o pet");
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            Pet pet;
            if (petId != null) {
                pet = petService.findById(petId)
                        .orElseThrow(() -> new IllegalStateException("Pet não encontrado."));
            } else {
                pet = new Pet();
            }

            Client selectedClient = clientField.getValue();
            if (selectedClient == null || selectedClient.getId() == 0) {
                Notification.show("Selecione um cliente.", 3000, Position.MIDDLE)
                        .addThemeNames("warning");
                clientField.focus();
                return;
            }

            pet.setClientId(selectedClient.getId());
            pet.setName(trimOrEmpty(nameField.getValue()));
            pet.setSpecies(trimOrEmpty(speciesField.getValue()));
            pet.setBreed(trimOrEmpty(breedField.getValue()));
            pet.setBirthDate(birthDatePicker.getValue());
            pet.setNotes(trimOrEmpty(notesArea.getValue()));

            if (petId != null) {
                petService.updateBasics(pet);
                Notification.show("Pet atualizado com sucesso.", 3000, Position.MIDDLE)
                        .addThemeNames("success");
            } else {
                long id = petService.create(pet);
                Notification.show("Pet criado com ID: " + id, 3000, Position.MIDDLE)
                        .addThemeNames("success");
            }

            UI.getCurrent().navigate("pets");

        } catch (PetValidationException vex) {
            Notification.show(vex.getMessage(), 4000, Position.MIDDLE)
                    .addThemeNames("error");
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao salvar pet: " + ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeNames("error");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro inesperado ao salvar pet: " + ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    private void loadClients() throws SQLException {
        List<Client> clients = clientService.listAllForCompany();
        clientField.setItems(clients);

        if (clients.isEmpty()) {
            saveBtn.setEnabled(false);
            Notification.show("Nenhum cliente encontrado. Cadastre um cliente antes de adicionar um pet.",
                            5000, Position.MIDDLE)
                    .addThemeNames("warning");
        } else {
            saveBtn.setEnabled(true);
        }
    }

    private void loadExistingPet(long petId) throws SQLException {
        var opt = petService.findById(petId);
        if (opt.isEmpty()) {
            throw new IllegalStateException("Pet não encontrado (id=" + petId + ").");
        }

        Pet pet = opt.get();

        nameField.setValue(nonNull(pet.getName()));
        speciesField.setValue(nonNull(pet.getSpecies()));
        breedField.setValue(nonNull(pet.getBreed()));
        notesArea.setValue(nonNull(pet.getNotes()));

        if (pet.getBirthDate() != null) birthDatePicker.setValue(pet.getBirthDate());
        else birthDatePicker.clear();

        long cid = pet.getClientId();
        if (cid != 0L) {
            clientField.getListDataView().getItems()
                    .filter(c -> c.getId() == cid)
                    .findFirst()
                    .ifPresent(clientField::setValue);
        }
    }

    private static String trimOrEmpty(String v) {
        return v == null ? "" : v.trim();
    }

    private static String nonNull(String v) {
        return v == null ? "" : v;
    }
}