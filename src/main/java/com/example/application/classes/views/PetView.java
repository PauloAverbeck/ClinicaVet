package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Client;
import com.example.application.classes.model.Pet;
import com.example.application.classes.service.ClientService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.PetService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

import java.sql.SQLException;
import java.util.List;

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

    private Long petId = null;

    public PetView(PetService petService,
                   ClientService clientService,
                   CurrentUserService currentUserService,
                   CurrentCompanyService currentCompanyService) {
        this.petService = petService;
        this.clientService = clientService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        var header = new ViewToolbar("Pet");
        add(header);

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("600px");

        clientField.setRequiredIndicatorVisible(true);
        clientField.setHelperText("Obrigatório");
        clientField.setItemLabelGenerator(Client::getName);

        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("Obrigatório");

        speciesField.setRequiredIndicatorVisible(true);
        speciesField.setHelperText("Obrigatório");

        breedField.setRequiredIndicatorVisible(true);
        breedField.setHelperText("Obrigatório");

        notesArea.setWidthFull();
        notesArea.setMaxLength(1000);
        notesArea.setHelperText("Observações gerais sobre o pet");

        form.add(clientField, nameField, speciesField, breedField, birthDatePicker, notesArea);
        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        content.add(saveBtn);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters params = event.getRouteParameters();
        params.getLong("id").ifPresent(id -> this.petId = id);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        try {
            ViewGuard.requireLogin(currentUserService, () -> {
                Notification.show("Faça login para continuar.", 3000, Position.MIDDLE)
                        .addThemeNames("error");
                UI.getCurrent().navigate("home");
            });

            currentCompanyService.activeCompanyIdOrThrow();

            loadClients();

            if (petId != null) {
                loadExistingPet(petId);
            }

        } catch (IllegalStateException ex) { // empresa não selecionada
            ex.printStackTrace();
            Notification.show("Selecione uma empresa para continuar.", 4000, Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("company/select");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar tela de pet: " + ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("home");
        }
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            Client selectedClient = clientField.getValue();
            String name = trimOrEmpty(nameField.getValue());
            String species = trimOrEmpty(speciesField.getValue());
            String breed = trimOrEmpty(breedField.getValue());

            if (selectedClient == null) {
                Notification.show("O cliente é obrigatório.", 3000, Position.MIDDLE)
                        .addThemeNames("error");
                clientField.focus();
                return;
            }
            long clientId = selectedClient.getId();

            if (name.isEmpty()) {
                Notification.show("O nome do pet é obrigatório.", 3000, Position.MIDDLE)
                        .addThemeNames("error");
                nameField.focus();
                return;
            }
            if (species.isEmpty()) {
                Notification.show("A espécie do pet é obrigatória.", 3000, Position.MIDDLE)
                        .addThemeNames("error");
                speciesField.focus();
                return;
            }
            if (breed.isEmpty()) {
                Notification.show("A raça do pet é obrigatória.", 3000, Position.MIDDLE)
                        .addThemeNames("error");
                breedField.focus();
                return;
            }

            if (petId != null) {
                Pet pet = petService.findById(petId)
                        .orElseThrow(() -> new IllegalStateException("Pet não encontrado."));

                pet.setClientId(clientId);
                pet.setName(name);
                pet.setSpecies(species);
                pet.setBreed(breed);
                pet.setBirthDate(
                        birthDatePicker.getValue() != null
                                ? birthDatePicker.getValue().atStartOfDay()
                                : null
                );
                pet.setNotes(notesArea.getValue());

                petService.updateBasics(pet);
                Notification.show("Pet atualizado com sucesso.", 3000, Position.MIDDLE)
                        .addThemeNames("success");

            } else {
                Pet pet = new Pet();
                pet.setClientId(clientId);
                pet.setName(name);
                pet.setSpecies(species);
                pet.setBreed(breed);
                pet.setBirthDate(
                        birthDatePicker.getValue() != null
                                ? birthDatePicker.getValue().atStartOfDay()
                                : null
                );
                pet.setNotes(notesArea.getValue());

                long id = petService.create(pet);
                Notification.show("Pet criado com ID: " + id, 3000, Position.MIDDLE)
                        .addThemeNames("success");
            }

            UI.getCurrent().navigate("pets");

        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            Notification.show(ex.getMessage(), 4000, Position.MIDDLE)
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

    private void loadClients() {
        try {
            List<Client> clients = clientService.listAllForCompany();
            clientField.setItems(clients);
            if (clients.isEmpty()) {
                Notification.show("Nenhum cliente encontrado. Cadastre um cliente antes de adicionar um pet.", 5000, Position.MIDDLE)
                        .addThemeNames("warning");
                UI.getCurrent().navigate("clients/new");
                saveBtn.setEnabled(false);
            } else {
                saveBtn.setEnabled(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar clientes: " + ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeNames("error");
            clientField.setItems(List.of());
            saveBtn.setEnabled(false);
        }
    }

    private void loadExistingPet(long petId) {
        try {
            var opt = petService.findById(petId);
            if (opt.isEmpty()) {
                Notification.show("Pet não encontrado (id=" + petId + ").", 4000, Position.MIDDLE)
                        .addThemeNames("error");
                UI.getCurrent().navigate("pets");
                return;
            }

            Pet pet = opt.get();

            nameField.setValue(pet.getName() != null ? pet.getName() : "");
            speciesField.setValue(pet.getSpecies() != null ? pet.getSpecies() : "");
            breedField.setValue(pet.getBreed() != null ? pet.getBreed() : "");

            if (pet.getBirthDate() != null) {
                birthDatePicker.setValue(pet.getBirthDate().toLocalDate());
            } else {
                birthDatePicker.clear();
            }

            notesArea.setValue(pet.getNotes() != null ? pet.getNotes() : "");

            if (pet.getClientId() != 0L) {
                clientField.getListDataView().getItems()
                        .filter(c -> c.getId() == pet.getClientId())
                        .findFirst()
                        .ifPresent(clientField::setValue);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao carregar pet: " + ex.getMessage(), 5000, Position.MIDDLE)
                    .addThemeNames("error");
            UI.getCurrent().navigate("pets");
        }
    }

    private static String trimOrEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}