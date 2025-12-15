package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.DocumentType;
import com.example.application.classes.service.CompanyService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Empresas")
@Route(value = "company/new", layout = MainLayout.class)
@Menu(title = "Cadastrar Empresa", icon = "la la-building", order = 6)
public class CompanyView extends VerticalLayout {
    private final CompanyService companyService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final TextField name = new TextField("Nome da empresa");
    private final ComboBox<DocumentType> cbDocType = new ComboBox<>("Tipo de documento");
    private final TextField document = new TextField("Documento");
    private final Button saveBtn = new Button("Salvar");

    @Autowired
    public CompanyView(CompanyService companyService, CurrentUserService currentUserService, CurrentCompanyService currentCompanyService) {
        this.companyService = companyService;
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        setSpacing(true);

        var header = new ViewToolbar("Cadastrar Empresa");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        H1 title = new H1("Cadastrar empresa");

        cbDocType.setItems(DocumentType.values());
        cbDocType.setRequiredIndicatorVisible(true);
        name.setRequiredIndicatorVisible(true);

        cbDocType.addValueChangeListener(e -> {
            document.clear();
            document.setHelperText(e.getValue() == DocumentType.CPF ? "Apenas 11 números"
                    : e.getValue() == DocumentType.CNPJ ? "Apenas 14 números"
                    : "5 a 9 caracteres alfanuméricos");
        });

        saveBtn.addClickShortcut(Key.ENTER);
        saveBtn.addClickListener(e -> onSave());
        saveBtn.addThemeNames("primary");

        FormLayout form = new FormLayout(name, cbDocType, document, saveBtn);
        form.setMaxWidth("640px");
        content.add(title, form);
    }

    private void onSave() {
        try {
            long userId = currentUserService.requireUserId();
            long id = companyService.createForUser(userId, name.getValue(), cbDocType.getValue(), document.getValue());
            Notification.show("Empresa criada com sucesso! ID: " + id, 5000, Notification.Position.TOP_CENTER)
                    .addThemeNames("success");
            name.clear();
            cbDocType.clear();
            document.clear();
        } catch (Exception ex) {
            Notification.show("Erro ao criar empresa: " + ex.getMessage(), 7000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
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
