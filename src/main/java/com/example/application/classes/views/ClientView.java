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
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;

@PageTitle("Cliente")
@Route(value = "clients/new", layout = MainLayout.class)
@RouteAlias(value = "clients/:id/edit", layout = MainLayout.class)
@Menu(title = "Cadastro de Cliente", icon = "la la-user-plus", order = 9)
public class ClientView extends Main implements BeforeEnterObserver {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final TextField nameField = new TextField("Nome");
    private final TextField emailField = new TextField("E-mail");
    private final TextField phoneField = new TextField("Telefone");
    private final TextArea notesArea = new TextArea("Notas");
    private final Button saveBtn = new Button("Salvar");

    private Long clientId = null;
    private boolean editing = false;

    public ClientView(ClientService clientService, CurrentUserService currentUserService, CurrentCompanyService currentCompanyService) {
        this.clientService = clientService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        var header = new ViewToolbar("Novo Cliente");
        add(header);

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("600px");

        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("Obrigatório");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setHelperText("voce@exemplo.com");
        notesArea.setWidthFull();
        notesArea.setHeight("120px");

        form.add(nameField, emailField, phoneField, notesArea);
        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        content.add(saveBtn);
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            String name = nameField.getValue().trim();
            if (name.isEmpty()) {
                Notification.show("Nome do cliente é obrigatório.", 5000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
                nameField.focus();
                return;
            }

            String email = emailField.getValue().trim();
            if (email.isEmpty()) {
                Notification.show("E-mail é obrigatório.", 5000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
                emailField.focus();
                return;
            }

            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                Notification.show("E-mail inválido.", 5000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
                emailField.focus();
                return;
            }

            String phone = phoneField.getValue().trim();
            if (!phone.isEmpty() && !phone.matches("^(\\+55\\s?)?(\\(?\\d{2}\\)?\\s?)?(9?\\d{4}[- ]?\\d{4})$")) {
                Notification.show("Telefone inválido.", 5000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
                phoneField.focus();
                return;
            }

            if (editing && clientId != null) {
                Client client = clientService.findById(clientId)
                        .orElseThrow(() -> new IllegalStateException("Cliente não encontrado (id= " + clientId + ")."));
                client.setName(nameField.getValue());
                client.setEmail(emailField.getValue());
                client.setPhone(phoneField.getValue());
                client.setNotes(notesArea.getValue());

                clientService.updateBasics(client);
                Notification.show("Cliente atualizado com sucesso.")
                        .addThemeNames("success");

            } else {
                Client client = new Client();
                client.setName(nameField.getValue());
                client.setEmail(emailField.getValue());
                client.setPhone(phoneField.getValue());
                client.setNotes(notesArea.getValue());

                long id = clientService.create(client);
                Notification.show("Cliente criado com ID: " + id)
                    .addThemeNames("success");
            }

            UI.getCurrent().navigate("clients");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao criar cliente: " + ex.getMessage(), 7000, Notification.Position.TOP_CENTER)
                .addThemeNames("error");
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().get("id").ifPresent(idStr -> {
            try {
                this.clientId = Long.parseLong(idStr);
                this.editing = true;
                loadExistingClient();
            } catch (NumberFormatException ex) {
                Notification.show("ID de cliente inválido: ", 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
                event.rerouteTo("clients");
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Erro ao carregar cliente: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
                event.rerouteTo("clients");
            }
        });
    }

    private void loadExistingClient() throws Exception {
        if (clientId == null) {
            return;
        }

        currentCompanyService.activeCompanyIdOrThrow();

        var opt = clientService.findById(clientId);
        if (opt.isEmpty()) {
            throw new IllegalStateException("Cliente não encontrado (id= "+ clientId + ").");
        }

        Client client = opt.get();
        nameField.setValue(client.getName() != null ? client.getName() : "");
        emailField.setValue(client.getEmail() != null ? client.getEmail() : "");
        phoneField.setValue(client.getPhone() != null ? client.getPhone() : "");
        notesArea.setValue(client.getNotes() != null ? client.getNotes() : "");

        saveBtn.setText("Salvar alterações");
    }
}
