package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.AppUser;
import com.example.application.classes.service.*;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@PageTitle("Usuários")
@Route(value = "users", layout = MainLayout.class)
@Menu(title = "Usuários", icon = "la la-users", order = 5)
public class AppUserView extends Main implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(AppUserView.class);

    private final AppUserService userService;
    private final UserCompanyService userCompanyService;
    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<CompanyUserRow> grid = new Grid<>(CompanyUserRow.class, false);

    private final Button editBtn = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");

    public AppUserView(AppUserService userService,
                       UserCompanyService userCompanyService,
                       CurrentUserService currentUserService,
                       CurrentCompanyService currentCompanyService) {

        this.userService = Objects.requireNonNull(userService);
        this.userCompanyService = Objects.requireNonNull(userCompanyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.currentCompanyService = Objects.requireNonNull(currentCompanyService);

        add(new ViewToolbar("Usuários"));

        configureGrid();
        add(grid);

        configureActionsBar();
        var actionsLayout = new HorizontalLayout(editBtn, deleteBtn);
        actionsLayout.setPadding(true);
        add(actionsLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUserService.isLoggedIn()) {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("home");
            return;
        }
        if (!currentCompanyService.hasSelection()) {
            Notification.show("Selecione uma empresa para continuar.", 3000, Notification.Position.MIDDLE);
            event.rerouteTo("company/select");
            return;
        }
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        updateActionButtonsState(false);
        reloadGrid();
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(CompanyUserRow::getUserId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(CompanyUserRow::getName)
                .setHeader("Nome")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(CompanyUserRow::getEmail)
                .setHeader("E-mail")
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(row -> row.isAdmin() ? "Sim" : "Não")
                .setHeader("Admin")
                .setAutoWidth(true)
                .setSortable(true);
    }

    private void configureActionsBar() {
        editBtn.addThemeNames("primary");
        deleteBtn.addThemeNames("error");

        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        grid.asSingleSelect().addValueChangeListener(e -> {
            boolean hasSelection = e.getValue() != null;
            updateActionButtonsState(hasSelection);
        });

        editBtn.addClickListener(e -> onEditSelected());
        deleteBtn.addClickListener(e -> onDeleteSelected());
    }

    private void updateActionButtonsState(boolean hasSelection) {
        boolean isAdmin = currentCompanyService.isAdmin();
        editBtn.setEnabled(isAdmin && hasSelection);
        deleteBtn.setEnabled(isAdmin && hasSelection);
    }

    private void reloadGrid() {
        try {
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            List<CompanyUserRow> rows = userCompanyService.listCompanyUsers(companyId);
            grid.setItems(rows);
        } catch (SQLException ex) {
            log.error("Erro ao listar usuários", ex);
            Notification.show("Erro ao listar usuários.",
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            grid.setItems(List.of());
        }
    }

    private void onEditSelected() {
        if (!currentCompanyService.isAdmin()) {
            Notification.show("Ação permitida apenas para administradores.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            return;
        }

        CompanyUserRow selectedRow = grid.asSingleSelect().getValue();
        if (selectedRow == null) {
            Notification.show("Selecione um usuário.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            AppUser selected = userService.findByIdOrThrow(selectedRow.getUserId());

            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Editar usuário");

            TextField nameField = new TextField("Nome");
            nameField.setWidthFull();
            nameField.setValue(selected.getName() != null ? selected.getName() : "");

            EmailField emailField = new EmailField("E-mail");
            emailField.setWidthFull();
            emailField.setValue(selected.getEmail() != null ? selected.getEmail() : "");
            emailField.setErrorMessage("Informe um e-mail válido");

            dialog.add(nameField, emailField);

            Button save = new Button("Salvar", ev -> {
                String newName = trimOrNull(nameField.getValue());
                String newEmail = trimOrNull(emailField.getValue());

                if (newName == null || newName.isBlank()) {
                    Notification.show("Nome é obrigatório.", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (newEmail == null || newEmail.isBlank() || emailField.isInvalid()) {
                    Notification.show("Informe um e-mail válido.", 3000, Notification.Position.MIDDLE);
                    return;
                }

                try {
                    selected.setName(newName);
                    selected.setEmail(newEmail);
                    userService.updateBasics(selected);

                    Notification.show("Usuário atualizado com sucesso.", 3000, Notification.Position.MIDDLE)
                            .addThemeNames("success");

                    dialog.close();
                    reloadGrid();
                } catch (Exception ex) {
                    log.error("Erro ao atualizar usuário", ex);
                    Notification.show("Erro ao atualizar usuário.",
                                    5000, Notification.Position.MIDDLE)
                            .addThemeNames("error");
                }
            });
            save.addThemeNames("success");

            Button cancel = new Button("Cancelar", ev -> dialog.close());
            dialog.getFooter().add(cancel, save);
            dialog.open();

        } catch (Exception ex) {
            log.error("Erro ao carregar usuário", ex);
            Notification.show("Erro ao carregar usuário.",
                            5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    private void onDeleteSelected() {
        if (!currentCompanyService.isAdmin()) {
            Notification.show("Ação permitida apenas para administradores.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            return;
        }

        CompanyUserRow selectedRow = grid.asSingleSelect().getValue();
        if (selectedRow == null) {
            Notification.show("Selecione um usuário.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            userService.deleteById(selectedRow.getUserId());
            Notification.show("Usuário removido com sucesso.", 3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");
            reloadGrid();
        } catch (Exception ex) {
            log.error("Erro ao remover usuário", ex);
            Notification.show("Erro ao remover usuário.",
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    private static String trimOrNull(String v) {
        return v == null ? null : v.trim();
    }
}
