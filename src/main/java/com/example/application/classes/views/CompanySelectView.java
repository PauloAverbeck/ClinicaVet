package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.Company;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;

@PageTitle("Selecionar empresa")
@Route(value = "company/select", layout = MainLayout.class)
@Menu(title = "Selecionar empresa", icon = "la la-building", order = 2)
public class CompanySelectView extends VerticalLayout {

    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<Company> grid = new Grid<>(Company.class, false);
    private final Button confirmBtn = new Button("Usar esta empresa");
    private final Button refreshBtn = new Button("Atualizar lista");
    private final Button createBtn  = new Button("Criar empresa");

    public CompanySelectView(CurrentUserService currentUserService,
                             CurrentCompanyService currentCompanyService) {
        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;

        setSizeFull();
        setSpacing(true);

        var header = new ViewToolbar("Selecionar empresa");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();
        content.setWidthFull();
        content.getStyle().set("display", "flex");
        content.getStyle().set("flex-direction", "column");
        content.getStyle().set("gap", "var(--lumo-space-m)");

        H1 title = new H1("Escolha com qual empresa você deseja trabalhar");

        grid.addColumn(Company::getName).setHeader("Nome").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Company::getDocument).setHeader("Documento").setAutoWidth(true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setHeight("420px");

        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmBtn.addClickListener(e -> onConfirm());
        refreshBtn.addClickListener(e -> {
            try { loadData(); } catch (Exception ex) { showError(ex); }
        });

        createBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
        createBtn.addClickListener(e -> UI.getCurrent().navigate("companies/new"));

        HorizontalLayout actions = new HorizontalLayout(confirmBtn, refreshBtn, createBtn);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        content.add(title, new Paragraph("Selecione uma empresa e clique em “Usar esta empresa”."), grid, actions);

        try {
            long uid = currentUserService.requireUserId(); // << substituído aqui
            if (currentCompanyService.ensureAutoSelectionIfSingle(uid)) {
                Notification.show("Empresa selecionada automaticamente.", 2500, Notification.Position.MIDDLE);
                UI.getCurrent().navigate("home");
                return;
            }
            loadData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void loadData() throws SQLException {
        grid.setItems(List.of());
        long uid = currentUserService.requireUserId(); // << substituído aqui
        List<Company> items = currentCompanyService.listSelectableForUser(uid);
        grid.setItems(items);

        boolean empty = items.isEmpty();
        confirmBtn.setEnabled(!empty);
        if (empty) {
            Notification.show("Você ainda não possui empresas. Crie uma para continuar.", 4000, Notification.Position.BOTTOM_CENTER);
        }
    }

    private void onConfirm() {
        Company selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("Selecione uma empresa.", 2500, Notification.Position.MIDDLE);
            return;
        }
        try {
            long uid = currentUserService.requireUserId(); // << substituído aqui
            currentCompanyService.selectCompanyForUser(uid, selected.getId());
            Notification.show("Empresa selecionada: " + selected.getName(), 2500, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("home");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        Notification.show("Falha: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
    }
}