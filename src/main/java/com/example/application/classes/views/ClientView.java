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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Novo Cliente")
@Route(value = "clients/new", layout = MainLayout.class)
@Menu(title = "Novo Cliente", icon = "la la-user-plus", order = 9)
public class ClientView extends Main {

    private final ClientService clientService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final TextField nameField = new TextField("Nome");
    private final TextField emailField = new TextField("E-mail");
    private final TextField phoneField = new TextField("Telefone");
    private final TextArea notesArea = new TextArea("Notas");
    private final Button saveBtn = new Button("Salvar");


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

        form.add(nameField, emailField, phoneField, notesArea);
        content.add(form);

        saveBtn.addThemeNames("primary");
        saveBtn.addClickListener(e -> onSave());
        content.add(saveBtn);
    }

    private void onSave() {
        try {
            Client client = new Client();
            client.setName(nameField.getValue());
            client.setEmail(emailField.getValue());
            client.setPhone(phoneField.getValue());
            client.setNotes(notesArea.getValue());

            long id = clientService.create(client);
            Notification.show("Cliente criado com ID: " + id)
                .addThemeNames("success");

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

        ViewGuard.requireLogin(currentUserService, () -> {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("home");
        });
        currentCompanyService.activeCompanyIdOrThrow();
    }
}
