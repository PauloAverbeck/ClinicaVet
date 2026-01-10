package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.DocumentType;
import com.example.application.classes.service.CompanyService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@PageTitle("Cadastrar Empresa")
@Route(value = "company/new", layout = MainLayout.class)
@Menu(title = "Cadastrar Empresa", icon = "la la-building", order = 6)
public class CompanyView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(CompanyView.class);

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final TextField name = new TextField("Nome da empresa");
    private final ComboBox<DocumentType> cbDocType = new ComboBox<>("Tipo de documento");
    private final TextField document = new TextField("Documento");
    private final Button saveBtn = new Button("Salvar");

    public CompanyView(CompanyService companyService,
                       CurrentUserService currentUserService,
                       CurrentCompanyService currentCompanyService) {

        this.companyService = Objects.requireNonNull(companyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.currentCompanyService = Objects.requireNonNull(currentCompanyService);

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        setSpacing(true);

        add(new ViewToolbar("Cadastrar Empresa"));

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
            var dt = e.getValue();
            if (dt == null) {
                document.setHelperText("");
                return;
            }
            switch (dt) {
                case CPF -> document.setHelperText("Apenas 11 números");
                case CNPJ -> document.setHelperText("Apenas 14 números");
                case PASSPORT -> document.setHelperText("5 a 9 caracteres alfanuméricos");
            }
        });

        saveBtn.addThemeNames("primary");
        saveBtn.addClickShortcut(Key.ENTER);
        saveBtn.addClickListener(e -> onSave());

        FormLayout form = new FormLayout(name, cbDocType, document, saveBtn);
        form.setMaxWidth("640px");

        content.add(title, form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUserService.isLoggedIn()) {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("home");
        }
    }

    private void onSave() {
        try {
            String n = trim(name.getValue());
            DocumentType dt = cbDocType.getValue();
            String doc = trim(document.getValue());

            if (n == null || n.isBlank()) {
                Notification.show("Informe o nome da empresa.", 3000, Notification.Position.MIDDLE);
                name.focus();
                return;
            }
            if (dt == null) {
                Notification.show("Selecione o tipo de documento.", 3000, Notification.Position.MIDDLE);
                cbDocType.focus();
                return;
            }
            if (doc == null || doc.isBlank()) {
                Notification.show("Informe o documento.", 3000, Notification.Position.MIDDLE);
                document.focus();
                return;
            }

            long userId = currentUserService.requireUserId();
            long id = companyService.createForCurrentUser(n, dt, doc);

            Notification.show("Empresa criada com sucesso! ID: " + id, 4000, Notification.Position.TOP_CENTER)
                    .addThemeNames("success");

            currentCompanyService.ensureAutoSelectionIfSingle(userId);

            UI.getCurrent().navigate("company/select");
        } catch (Exception ex) {
            log.error("Erro ao criar empresa", ex);
            Notification.show("Erro ao criar empresa.", 7000, Notification.Position.TOP_CENTER)
                    .addThemeNames("error");
        }
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }
}
