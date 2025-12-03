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
import com.vaadin.flow.component.combobox.ComboBox;
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

    // Dados básicos
    private final TextField nameField = new TextField("Nome");
    private final TextField emailField = new TextField("E-mail");
    private final TextField phoneField = new TextField("Telefone");

    private final ComboBox<String> docTypeField = new ComboBox<>("Tipo de documento");
    private final TextField documentField = new TextField("Documento");

    // Endereço
    private final TextField cepField = new TextField("CEP");
    private final TextField ufField = new TextField("UF");
    private final TextField cityField = new TextField("Cidade");
    private final TextField districtField = new TextField("Bairro");
    private final TextField streetField = new TextField("Rua");
    private final TextField numberField = new TextField("Número");
    private final TextField complementField = new TextField("Complemento");

    // Observações
    private final TextArea notesArea = new TextArea("Notas");

    private final Button saveBtn = new Button("Salvar");

    private Long clientId = null;
    private boolean editing = false;

    public ClientView(
            ClientService clientService,
            CurrentUserService currentUserService,
            CurrentCompanyService currentCompanyService
    ) {
        this.clientService = clientService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        var header = new ViewToolbar("Cliente");
        add(header);

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("800px");

        // Configuração campos básicos
        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("Obrigatório");

        emailField.setRequiredIndicatorVisible(true);
        emailField.setHelperText("voce@exemplo.com");

        notesArea.setWidthFull();
        notesArea.setHeight("120px");

        // Tipo de documento
        docTypeField.setItems("CPF", "CNPJ", "Passaporte");
        docTypeField.setPlaceholder("Selecione");
        docTypeField.setClearButtonVisible(true);

        cepField.setMaxLength(20);
        ufField.setMaxLength(2);

        // Layout do formulário
        form.add(
                nameField, emailField,
                phoneField,
                docTypeField, documentField,
                cepField, ufField,
                cityField, districtField,
                streetField, numberField,
                complementField,
                notesArea
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        content.add(saveBtn);
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            // Validações básicas
            String name = safeTrim(nameField.getValue());
            if (name.isEmpty()) {
                showError("Nome do cliente é obrigatório.");
                nameField.focus();
                return;
            }

            String email = safeTrim(emailField.getValue());
            if (email.isEmpty()) {
                showError("E-mail é obrigatório.");
                emailField.focus();
                return;
            }

            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                showError("E-mail inválido.");
                emailField.focus();
                return;
            }

            String phone = safeTrim(phoneField.getValue());
            if (!phone.isEmpty()
                    && !phone.matches("^(\\+55\\s?)?(\\(?\\d{2}\\)?\\s?)?(9?\\d{4}[- ]?\\d{4})$")) {
                showError("Telefone inválido.");
                phoneField.focus();
                return;
            }

            String docType = docTypeField.getValue();
            String document = safeTrim(documentField.getValue());

            if (docType != null && !docType.isBlank() && document.isEmpty()) {
                showError("Preencha o documento para o tipo selecionado.");
                documentField.focus();
                return;
            }

            Client client;

            if (editing && clientId != null) {
                client = clientService.findById(clientId)
                        .orElseThrow(() -> new IllegalStateException("Cliente não encontrado (id=" + clientId + ")."));
            } else {
                client = new Client();
            }

            client.setName(name);
            client.setEmail(email);
            client.setPhone(phone);
            client.setNotes(safeTrim(notesArea.getValue()));

            client.setDocType(docType);
            client.setDocument(document);

            client.setCep(safeTrim(cepField.getValue()));
            client.setUf(safeTrim(ufField.getValue()).toUpperCase());
            client.setCity(safeTrim(cityField.getValue()));
            client.setDistrict(safeTrim(districtField.getValue()));
            client.setStreet(safeTrim(streetField.getValue()));
            client.setNumber(safeTrim(numberField.getValue()));
            client.setComplement(safeTrim(complementField.getValue()));

            if (editing) {
                clientService.updateBasics(client);
                Notification.show("Cliente atualizado com sucesso.")
                        .addThemeNames("success");
            } else {
                long id = clientService.create(client);
                Notification.show("Cliente criado com ID: " + id)
                        .addThemeNames("success");
            }

            UI.getCurrent().navigate("clients");
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao salvar cliente: " + ex.getMessage(),
                            7000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_CENTER)
                .addThemeNames("error");
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
                saveBtn.setText("Salvar alterações");
            } catch (NumberFormatException ex) {
                Notification.show("ID de cliente inválido.", 5000, Notification.Position.MIDDLE)
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
            throw new IllegalStateException("Cliente não encontrado (id=" + clientId + ").");
        }

        Client client = opt.get();

        nameField.setValue(nonNull(client.getName()));
        emailField.setValue(nonNull(client.getEmail()));
        phoneField.setValue(nonNull(client.getPhone()));
        notesArea.setValue(nonNull(client.getNotes()));

        docTypeField.setValue(client.getDocType());
        documentField.setValue(nonNull(client.getDocument()));

        cepField.setValue(nonNull(client.getCep()));
        ufField.setValue(nonNull(client.getUf()));
        cityField.setValue(nonNull(client.getCity()));
        districtField.setValue(nonNull(client.getDistrict()));
        streetField.setValue(nonNull(client.getStreet()));
        numberField.setValue(nonNull(client.getNumber()));
        complementField.setValue(nonNull(client.getComplement()));
    }

    private static String nonNull(String value) {
        return value != null ? value : "";
    }
}
