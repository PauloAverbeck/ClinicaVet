package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.DocumentField;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Client;
import com.example.application.classes.service.*;
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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;

@PageTitle("Cliente")
@Route(value = "clients/new", layout = MainLayout.class)
@RouteAlias(value = "clients/:id/edit", layout = MainLayout.class)
@Menu(title = "Cadastro de Cliente", icon = "la la-user-plus", order = 9)
public class ClientView extends Main implements BeforeEnterObserver {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;
    private final ViaCepService viaCepService;

    private final TextField nameField = new TextField("Nome");
    private final TextField emailField = new TextField("E-mail");
    private final TextField phoneField = new TextField("Telefone");

    private final DocumentField documentField = new DocumentField();

    private final TextField cepField = new TextField("CEP");
    private final TextField ufField = new TextField("UF");
    private final TextField cityField = new TextField("Cidade");
    private final TextField districtField = new TextField("Bairro");
    private final TextField streetField = new TextField("Rua");
    private final TextField numberField = new TextField("Número");
    private final TextField complementField = new TextField("Complemento");

    private final TextArea notesArea = new TextArea("Notas");

    private final Button saveBtn = new Button("Salvar");

    private Long clientId = null;
    private boolean editing = false;

    public ClientView(ClientService clientService,
                      CurrentUserService currentUserService,
                      CurrentCompanyService currentCompanyService,
                      ViaCepService viaCepService) {
        this.clientService = clientService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;
        this.viaCepService = viaCepService;

        var header = new ViewToolbar("Cliente");
        add(header);

        var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);

        var form = new FormLayout();
        form.setMaxWidth("800px");

        configureFields();
        configureMasksAndCepAutoFill();

        form.add(
                nameField, emailField,
                phoneField,
                documentField,
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

    private void configureFields() {
        nameField.setRequiredIndicatorVisible(true);
        nameField.setHelperText("Obrigatório");
        nameField.setMaxLength(200);

        emailField.setRequiredIndicatorVisible(true);
        emailField.setHelperText("voce@exemplo.com");
        emailField.setMaxLength(320);

        phoneField.setMaxLength(32);
        phoneField.setValueChangeMode(ValueChangeMode.EAGER);

        cepField.setMaxLength(9);
        cepField.setValueChangeMode(ValueChangeMode.EAGER);

        ufField.setMaxLength(2);

        cityField.setMaxLength(100);
        districtField.setMaxLength(100);
        streetField.setMaxLength(150);
        numberField.setMaxLength(20);
        complementField.setMaxLength(150);

        notesArea.setWidthFull();
        notesArea.setHeight("120px");
    }

    private void configureMasksAndCepAutoFill() {
        phoneField.addValueChangeListener(e -> {
            String digits = e.getValue() == null ? "" : e.getValue().replaceAll("\\D", "");
            if (digits.length() > 11) digits = digits.substring(0, 11);

            String formatted = digits;
            if (digits.length() == 10) {
                formatted = digits.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
            } else if (digits.length() == 11) {
                formatted = digits.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
            }
            if (!formatted.equals(e.getValue())) {
                phoneField.setValue(formatted);
            }
        });

        cepField.addValueChangeListener(e -> {
            String raw = e.getValue() == null ? "" : e.getValue();
            String digits = raw.replaceAll("\\D", "");
            if (digits.length() > 8) digits = digits.substring(0, 8);

            String formatted = digits;
            if (digits.length() > 5) {
                formatted = digits.replaceFirst("(\\d{5})(\\d+)", "$1-$2");
            }

            if (!formatted.equals(raw)) {
                cepField.setValue(formatted);
                return; // evita chamada dupla
            }

            if (digits.length() == 8) {
                lookupCepAndFill(digits);
            }
        });
    }

    private void lookupCepAndFill(String digits) {
        try {
            var opt = viaCepService.lookup(digits);
            if (opt.isEmpty()) {
                Notification.show("CEP não encontrado ou inválido.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeNames("warning");
                return;
            }

            ViaCepResponse dto = opt.get();
            if (cityField.isEmpty()) {
                cityField.setValue(nonNull(dto.getLocalidade()));
            }
            if (districtField.isEmpty()) {
                districtField.setValue(nonNull(dto.getBairro()));
            }
            if (streetField.isEmpty()) {
                streetField.setValue(nonNull(dto.getLogradouro()));
            }
            if (ufField.isEmpty()) {
                ufField.setValue(nonNull(dto.getUf()).toUpperCase());
            }
            if (complementField.isEmpty()) {
                complementField.setValue(nonNull(dto.getComplemento()));
            }

        } catch (Exception ex) {
            ex.printStackTrace(); // opcional
            Notification.show("Erro ao consultar CEP.", 3000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
        }
    }

    private void onSave() {
        try {
            currentCompanyService.activeCompanyIdOrThrow();

            Client client;
            if (editing && clientId != null) {
                client = clientService.findById(clientId)
                        .orElseThrow(() -> new IllegalStateException("Cliente não encontrado (id=" + clientId + ")."));
            } else {
                client = new Client();
            }

            client.setName(safeTrim(nameField.getValue()));
            client.setEmail(safeTrim(emailField.getValue()));
            client.setPhone(safeTrim(phoneField.getValue()));

            client.setDocType(documentField.getDocType());
            client.setDocument(safeTrim(documentField.getValue()));

            client.setCep(safeTrim(cepField.getValue()));
            client.setUf(safeTrim(ufField.getValue()).toUpperCase());
            client.setCity(safeTrim(cityField.getValue()));
            client.setDistrict(safeTrim(districtField.getValue()));
            client.setStreet(safeTrim(streetField.getValue()));
            client.setNumber(safeTrim(numberField.getValue()));
            client.setComplement(safeTrim(complementField.getValue()));

            client.setNotes(safeTrim(notesArea.getValue()));

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
        } catch (ClientValidationException vex) {
            showError(vex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erro ao salvar cliente: " + ex.getMessage());
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.TOP_CENTER)
                .addThemeNames("error");
    }

    private static String nonNull(String value) {
        return value != null ? value : "";
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

        documentField.setDocType(client.getDocType());
        documentField.setValue(nonNull(client.getDocument())); // DocumentField já formata

        String cep = client.getCep();
        if (cep != null && !cep.isBlank()) {
            String digits = cep.replaceAll("\\D", "");
            if (digits.matches("\\d{8}")) {
                cep = digits.replaceFirst("(\\d{5})(\\d{3})", "$1-$2");
            }
        }
        cepField.setValue(nonNull(cep));

        ufField.setValue(nonNull(client.getUf()));
        cityField.setValue(nonNull(client.getCity()));
        districtField.setValue(nonNull(client.getDistrict()));
        streetField.setValue(nonNull(client.getStreet()));
        numberField.setValue(nonNull(client.getNumber()));
        complementField.setValue(nonNull(client.getComplement()));
    }
}