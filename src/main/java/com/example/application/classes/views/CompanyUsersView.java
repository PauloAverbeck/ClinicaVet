package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.service.CompanyUserRow;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;

@PageTitle("Usuários da Empresa")
@Route(value = "company/users", layout = MainLayout.class)
@Menu(title = "Usuários da Empresa", icon = "la la-users", order = 7)
public class CompanyUsersView extends Main  {
    private CurrentUserService currentUserService;
    private CurrentCompanyService currentCompanyService;
    private UserCompanyService userCompanyService;

    private final Grid<CompanyUserRow> grid;
    private final Button addUserBtn = new Button("Adicionar Usuário");

    public CompanyUsersView(
            CurrentUserService currentUserService,
            CurrentCompanyService currentCompanyService,
            UserCompanyService userCompanyService
    ) {

        this.currentUserService = currentUserService;
        this.currentCompanyService = currentCompanyService;
        this.userCompanyService = userCompanyService;

        String companyName = currentCompanyService.activeCompanyNameOrNull();
        var header = new ViewToolbar("Usuários da Empresa: " + companyName);
        add(header);

        this.grid = buildGrid();
        add(this.grid);

        addUserBtn.addThemeNames("primary");
        addUserBtn.addClickListener(e -> openAddUserDialog());

        HorizontalLayout actionsBar = new HorizontalLayout(addUserBtn);
        actionsBar.setPadding(true);
        add(actionsBar);
    }

    private Grid<CompanyUserRow> buildGrid() {
        final var grid = new Grid<CompanyUserRow>(CompanyUserRow.class, false);
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

        grid.addColumn(new ComponentRenderer<>( row -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button adminBtn = new Button(row.isAdmin() ? "Remover Admin" : "Tornar Admin");
            adminBtn.addClickListener(event -> {
                try {
                    userCompanyService.setAdmin(row.getUserId(), currentCompanyService.activeCompanyIdOrThrow(), !row.isAdmin());
                    Notification.show("Permissão de admin atualizada com sucesso.", 3000, Notification.Position.MIDDLE)
                            .addThemeNames("success");
                    reloadGrid();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Notification.show("Erro ao atualizar permissão de admin: " + ex.getMessage(),
                                    6000, Notification.Position.MIDDLE)
                            .addThemeNames("error");
                }
            });

            Button removeBtn = new Button("Remover");
            removeBtn.addThemeNames("error");
            removeBtn.addClickListener(event -> {
                try {
                    userCompanyService.unlink(row.getUserId(), currentCompanyService.activeCompanyIdOrThrow());
                    Notification.show("Usuário removido com sucesso.", 3000, Notification.Position.MIDDLE)
                            .addThemeNames("success");
                    reloadGrid();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Notification.show("Erro ao remover usuário: " + ex.getMessage(),
                                    6000, Notification.Position.MIDDLE)
                            .addThemeNames("error");
                }
            });
            actions.add(adminBtn, removeBtn);
            return actions;
        })).setHeader("Ações").setAutoWidth(true);

        return grid;
    }

    private void reloadGrid() {
        try {
            List<CompanyUserRow> companyUsers = userCompanyService.listCompanyUsers(currentCompanyService.activeCompanyIdOrThrow());
            grid.setItems(companyUsers);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Erro ao listar usuários: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            grid.setItems(List.of());
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

        ViewGuard.requireAdmin(currentUserService, currentCompanyService, userCompanyService, () -> {
            Notification.show("Acesso negado. Apenas administradores podem acessar esta página.",
                    4000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("home");
        });
    }

    private void openAddUserDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Adicionar Usuário à Empresa");

        TextField emailField = new TextField("E-mail do Usuário");
        emailField.setWidthFull();
        emailField.setPlaceholder("exemplo@empresa.com");

        Button addBtn = new Button("Adicionar");
        addBtn.addThemeNames("primary");

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        addBtn.addClickListener(e -> {
            try {
                String email = emailField.getValue().trim();
                if (email.isEmpty()) {
                    Notification.show("O e-mail não pode estar vazio.", 3000, Notification.Position.MIDDLE)
                            .addThemeNames("error");
                    return;
                }

                long companyId = currentCompanyService.activeCompanyIdOrThrow();
                long addedUserId = userCompanyService.addUserByEmailToCompany(email, companyId);

                Notification.show("Usuário vinculado com sucesso!", 3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");

                dialog.close();
                reloadGrid();

            } catch (IllegalStateException ex) {
                Notification.show(ex.getMessage(), 6000, Notification.Position.MIDDLE)
                        .addThemeNames("error");

            } catch (SQLException ex) {
                ex.printStackTrace();
                Notification.show("Erro ao adicionar usuário: " + ex.getMessage(),
                                6000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
            }
        });

        dialog.getFooter().add(cancelBtn, addBtn);

        dialog.add(emailField);
        dialog.open();
    }
}
