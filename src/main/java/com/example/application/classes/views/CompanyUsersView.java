package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.CompanyUserRow;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@PageTitle("Usuários da Empresa")
@Route(value = "company/users", layout = MainLayout.class)
@Menu(title = "Usuários da Empresa", icon = "la la-users", order = 7)
public class CompanyUsersView extends Main implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(CompanyUsersView.class);

    private final CurrentUserService currentUserService;
    private final CurrentCompanyService currentCompanyService;
    private final UserCompanyService userCompanyService;

    private final ViewToolbar header = new ViewToolbar("Usuários da Empresa");

    private final Grid<CompanyUserRow> grid = new Grid<>(CompanyUserRow.class, false);
    private final Button addUserBtn = new Button("Adicionar Usuário");

    public CompanyUsersView(CurrentUserService currentUserService,
                            CurrentCompanyService currentCompanyService,
                            UserCompanyService userCompanyService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.currentCompanyService = Objects.requireNonNull(currentCompanyService);
        this.userCompanyService = Objects.requireNonNull(userCompanyService);

        add(header);

        configureGrid();
        configureActions();

        var actionsBar = new HorizontalLayout(addUserBtn);
        actionsBar.setPadding(true);

        add(grid, actionsBar);
        setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        ViewGuard.requireAdmin(event, currentUserService, currentCompanyService, userCompanyService);

        if (!currentUserService.isLoggedIn()) return;
        if (!currentCompanyService.hasSelection()) return;
        if (!isCurrentUserAdminSafely()) {
            Notification.show("Acesso restrito a administradores da empresa.",
                            3000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            event.rerouteTo("users");
        };

        updateHeader();
        reloadGrid();
    }

    private boolean isCurrentUserAdminSafely() {
        try {
            long userId = currentUserService.requireUserId();
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            return userCompanyService.isAdmin(userId, companyId);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void updateHeader() {
        String companyName = currentCompanyService.activeCompanyNameOrNull();
        if (companyName == null || companyName.isBlank()) {
            header.setTitle("Usuários da Empresa");
        } else {
            header.setTitle("Usuários da Empresa: " + companyName);
        }
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
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(row -> row.isAdmin() ? "Sim" : "Não")
                .setHeader("Admin")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(new ComponentRenderer<>(this::buildRowActions))
                .setHeader("Ações")
                .setAutoWidth(true);
    }

    private HorizontalLayout buildRowActions(CompanyUserRow row) {
        var actions = new HorizontalLayout();
        actions.setPadding(false);
        actions.setSpacing(true);

        Button adminBtn = new Button(row.isAdmin() ? "Remover Admin" : "Tornar Admin");
        adminBtn.addClickListener(e -> onToggleAdmin(row));

        Button removeBtn = new Button("Remover");
        removeBtn.addThemeNames("error");
        removeBtn.addClickListener(e -> onRemoveUser(row));

        actions.add(adminBtn, removeBtn);
        return actions;
    }

    private void configureActions() {
        addUserBtn.addThemeNames("primary");
        addUserBtn.addClickListener(e -> openAddUserDialog());
    }

    private void reloadGrid() {
        try {
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            List<CompanyUserRow> companyUsers = userCompanyService.listCompanyUsers(companyId);
            grid.setItems(companyUsers);
        } catch (SQLException ex) {
            log.error("Erro ao listar usuários", ex);
            Notification.show("Erro ao listar usuários.",
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            grid.setItems(List.of());
        }
    }

    private void onToggleAdmin(CompanyUserRow row) {
        try {
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            userCompanyService.setAdmin(row.getUserId(), companyId, !row.isAdmin());

            Notification.show("Permissão de admin atualizada com sucesso.",
                            3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");

            reloadGrid();
        } catch (SQLException ex) {
            log.error("Erro ao atualizar permissão de admin", ex);
            Notification.show("Erro ao atualizar permissão de admin.",
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    private void onRemoveUser(CompanyUserRow row) {
        try {
            long companyId = currentCompanyService.activeCompanyIdOrThrow();
            userCompanyService.unlink(row.getUserId(), companyId);

            Notification.show("Usuário removido com sucesso.",
                            3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");

            reloadGrid();
        } catch (SQLException ex) {
            log.error("Erro ao remover usuário", ex);
            Notification.show("Erro ao remover usuário.",
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }
    }

    private void openAddUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Adicionar Usuário à Empresa");

        TextField emailField = new TextField("E-mail do Usuário");
        emailField.setWidthFull();
        emailField.setPlaceholder("exemplo@empresa.com");

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        Button addBtn = new Button("Adicionar");
        addBtn.addThemeNames("primary");

        addBtn.addClickListener(e -> {
            String email = emailField.getValue() == null ? "" : emailField.getValue().trim();
            if (email.isBlank()) {
                Notification.show("O e-mail não pode estar vazio.",
                                3000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
                return;
            }

            try {
                long companyId = currentCompanyService.activeCompanyIdOrThrow();
                userCompanyService.addUserByEmailToCompany(email, companyId);

                Notification.show("Usuário vinculado com sucesso!",
                                3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");

                dialog.close();
                reloadGrid();
            } catch (IllegalStateException ex) {
                Notification.show(ex.getMessage(),
                                6000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
            } catch (SQLException ex) {
                log.error("Erro ao adicionar usuário", ex);
                Notification.show("Erro ao adicionar usuário.",
                                6000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
            }
        });

        dialog.add(emailField);
        dialog.getFooter().add(cancelBtn, addBtn);
        dialog.open();
    }
}
