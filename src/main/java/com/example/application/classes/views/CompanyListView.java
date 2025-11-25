package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.DocumentType;
import com.example.application.classes.model.Company;
import com.example.application.classes.service.CompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@PageTitle("Empresas")
@Route(value = "companies", layout = MainLayout.class)
@Menu(title = "Empresas", icon = "la la-building", order = 4)
// TODO: usar @RolesAllowed quando implementar controle de acesso
public class CompanyListView extends Main {

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;

    private final Grid<Company> grid;

    private final Button newBtn    = new Button("Nova empresa");
    private final Button editBtn   = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");

    public CompanyListView(CompanyService companyService,
                           CurrentUserService currentUserService) {
        this.companyService = Objects.requireNonNull(companyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);

        var header = new ViewToolbar("Empresas");
        add(header);

        this.grid = buildGrid();
        add(grid);

        configureActionsBar();
        var actionsLayout = new HorizontalLayout(newBtn, editBtn, deleteBtn);
        actionsLayout.setPadding(true);
        add(actionsLayout);

        reloadGrid();
    }

    private Grid<Company> buildGrid() {
        final var grid = new Grid<Company>(Company.class, false);
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(c -> c.getId() == 0 ? "-" : Long.toString(c.getId()))
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Company::getName)
                .setHeader("Nome")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(c -> c.getDocumentType() != null ? c.getDocumentType().name() : "-")
                .setHeader("Tipo Doc.")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(c -> c.getDocument() != null ? c.getDocument() : "-")
                .setHeader("Documento")
                .setAutoWidth(true)
                .setSortable(true);

        return grid;
    }

    private void configureActionsBar() {
        newBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        grid.asSingleSelect().addValueChangeListener(e -> {
            boolean hasSelection = e.getValue() != null;
            editBtn.setEnabled(hasSelection);
            deleteBtn.setEnabled(hasSelection);
        });

        newBtn.addClickListener(e -> onNew());
        editBtn.addClickListener(e -> onEditSelected());
        deleteBtn.addClickListener(e -> onDeleteSelected());
    }

    private void reloadGrid() {
        try {
            List<Company> companies = companyService.listAll();
            grid.setItems(companies);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao listar empresas: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            grid.setItems(List.of());
        }
    }

    /* === AÇÕES === */

    private void onNew() {
        UI.getCurrent().navigate("company/new");
    }

    /** Edita empresa selecionada (nome + documento/tipo). */
    private void onEditSelected() {
        Company selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione uma empresa.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar empresa");

        TextField nameField = new TextField("Nome da empresa");
        nameField.setWidthFull();
        nameField.setValue(selected.getName() != null ? selected.getName() : "");

        ComboBox<DocumentType> docTypeField = new ComboBox<>("Tipo de documento");
        docTypeField.setItems(DocumentType.values());
        docTypeField.setWidthFull();
        docTypeField.setRequiredIndicatorVisible(true);
        docTypeField.setValue(selected.getDocumentType());

        TextField documentField = new TextField("Documento");
        documentField.setWidthFull();
        documentField.setValue(selected.getDocument() != null ? selected.getDocument() : "");

        docTypeField.addValueChangeListener(e -> {
            var dt = e.getValue();
            if (dt == null) {
                documentField.setHelperText("");
                return;
            }
            switch (dt) {
                case CPF -> documentField.setHelperText("Apenas 11 números");
                case CNPJ -> documentField.setHelperText("Apenas 14 números");
                case PASSPORT -> documentField.setHelperText("5 a 9 caracteres alfanuméricos (sem especiais)");
            }
        });

        // Ajusta helper inicial
        if (selected.getDocumentType() != null) {
            switch (selected.getDocumentType()) {
                case CPF -> documentField.setHelperText("Apenas 11 números");
                case CNPJ -> documentField.setHelperText("Apenas 14 números");
                case PASSPORT -> documentField.setHelperText("5 a 9 caracteres alfanuméricos (sem especiais)");
            }
        }

        dialog.add(nameField, docTypeField, documentField);

        Button save = new Button("Salvar", ev -> {
            String newName = trimOrNull(nameField.getValue());
            DocumentType newType = docTypeField.getValue();
            String newDoc = trimOrNull(documentField.getValue());

            if (newName == null || newName.isBlank()) {
                Notification.show("Nome da empresa é obrigatório.", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (newType == null || newDoc == null || newDoc.isBlank()) {
                Notification.show("Tipo de documento e documento são obrigatórios.", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                selected.setName(newName);
                selected.setDocumentType(newType);
                selected.setDocument(newDoc);
                companyService.updateBasics(selected);

                Notification.show("Empresa atualizada com sucesso.",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                reloadGrid();
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Erro ao atualizar empresa: " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancelar", ev -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    /** Remove empresa selecionada (se o banco permitir). */
    private void onDeleteSelected() {
        Company selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == 0) {
            Notification.show("Selecione uma empresa.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Remover empresa");
        confirm.add("Tem certeza que deseja remover a empresa \"" +
                selected.getName() + "\" (ID " + selected.getId() + ")?");

        Button yes = new Button("Remover", ev -> {
            try {
                companyService.deleteById(selected.getId());
                Notification.show("Empresa removida com sucesso.",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                confirm.close();
                reloadGrid();
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Erro ao remover empresa: " + ex.getMessage(),
                                6000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button no = new Button("Cancelar", ev -> confirm.close());

        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    // TODO Remover quando implementar controle de acesso “de verdade”
    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        ViewGuard.requireLogin(currentUserService, () -> {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("home");
        });
    }

    private static String trimOrNull(String v) {
        return v == null ? null : v.trim();
    }
}