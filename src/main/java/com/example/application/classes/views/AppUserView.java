package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.AppUser;
import com.example.application.classes.model.Company;
import com.example.application.classes.service.*;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@PageTitle("Usuários")
@Route(value = "users", layout = MainLayout.class)
@Menu(title = "Usuários", icon = "la la-users", order = 5)
public class AppUserView extends Main {

    private final AppUserService userService;
    private final UserCompanyService userCompanyService;
    private final CurrentUserService currentUserService;
    private final CompanyService companyService;
    private final CurrentCompanyService currentCompanyService;

    private final Grid<AppUser> grid;

    private final Button editBtn = new Button("Editar");
    private final Button deleteBtn = new Button("Remover");

    public AppUserView(final AppUserService userService,
                       final UserCompanyService userCompanyService,
                       final CurrentUserService currentUserService,
                       final CompanyService companyService, CurrentCompanyService currentCompanyService) {
        this.userService = Objects.requireNonNull(userService);
        this.userCompanyService = Objects.requireNonNull(userCompanyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.companyService = Objects.requireNonNull(companyService);
        this.currentCompanyService = currentCompanyService;

        var header = new ViewToolbar("Usuários");
        add(header);

        this.grid = buildGrid();
        add(grid);

        configureActionsBar();
        var actionsLayout = new HorizontalLayout(editBtn, deleteBtn);
        actionsLayout.setPadding(true);
        add(actionsLayout);

        reloadGrid();
    }

    private Grid<AppUser> buildGrid() {
        final var grid = new Grid<>(AppUser.class, false);
        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(user -> user.getId() == null ? "-" : user.getId().toString())
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(AppUser::getName)
                .setHeader("Nome")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(AppUser::getEmail)
                .setHeader("E-mail")
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(new ComponentRenderer<Text, AppUser>(u -> new Text("")))
                .setHeader("Empresa(s)")
                .setKey("companies")
                .setAutoWidth(true)
                .setFlexGrow(1);

        return grid;
    }

    private void configureActionsBar() {
        editBtn.addThemeNames("primary");
        deleteBtn.addThemeNames("error");

        editBtn.setEnabled(false);
        deleteBtn.setEnabled(false);

        grid.asSingleSelect().addValueChangeListener(e -> {
            boolean hasSelection = e.getValue() != null;
            editBtn.setEnabled(hasSelection);
            deleteBtn.setEnabled(hasSelection);
        });

        editBtn.addClickListener(e -> onEditSelected());
        deleteBtn.addClickListener(e -> onDeleteSelected());
    }

    private void reloadGrid() {
        try {
            List<AppUser> users = userService.listAll();
            Map<Long, String> companiesMap = userCompanyService.companiesByUserIdAggregated();
            grid.setItems(users);

            var col = grid.getColumnByKey("companies");
            if (col != null) {
                col.setRenderer(new ComponentRenderer<Text, AppUser>(
                        u -> new Text(companiesMap.getOrDefault(u.getId(), ""))
                ));
            }

        } catch (SQLException ex) {
            Notification.show("Erro ao listar usuários: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
            grid.setItems(List.of());
        }
    }

    /** Edição de usuário selecionado (nome, e-mail, empresa). */
    private void onEditSelected() {
        AppUser selected = grid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("Selecione um usuário.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Editar usuário");

        TextField nameField = new TextField("Nome");
        nameField.setWidthFull();
        nameField.setValue(selected.getName() != null ? selected.getName() : "");

        EmailField emailField = new EmailField("E-mail");
        emailField.setWidthFull();
        emailField.setValue(selected.getEmail() != null ? selected.getEmail() : "");
        emailField.setErrorMessage("Informe um e-mail válido");

        MultiSelectComboBox<Company> companiesField = new MultiSelectComboBox<>("Empresa(s)");
        companiesField.setWidthFull();
        companiesField.setItemLabelGenerator(Company::getName);

        try {
            List<Company> allCompanies = companyService.listAll();
            companiesField.setItems(allCompanies);

            var choices = userCompanyService.companyChoicesFor(selected.getId());
            var currentCompanyIds = choices.stream()
                    .map(c -> c.id)
                    .collect(Collectors.toSet());

            var selectedCompanies = allCompanies.stream()
                    .filter(c -> currentCompanyIds.contains(c.getId()))
                    .collect(Collectors.toSet());

            companiesField.setValue(selectedCompanies);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Notification.show("Falha ao carregar empresas: " + ex.getMessage(),
                            5000, Notification.Position.MIDDLE)
                    .addThemeNames("error");
        }

        dialog.add(nameField, emailField, companiesField);

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

                var newCompanyIds = companiesField.getSelectedItems().stream()
                        .map(Company::getId)
                        .collect(Collectors.toSet());

                long actingUserId;
                try {
                    actingUserId = currentUserService.requireUserId();
                } catch (Exception ex) {
                    actingUserId = selected.getId();
                }

                userCompanyService.replaceCompaniesForUser(actingUserId, selected.getId(), newCompanyIds);

                Notification.show("Usuário atualizado com sucesso.",
                                3000, Notification.Position.MIDDLE)
                        .addThemeNames("success");
                dialog.close();
                reloadGrid();
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Erro ao atualizar usuário: " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE)
                        .addThemeNames("error");
            }
        });
        save.addThemeNames("success");

        Button cancel = new Button("Cancelar", ev -> dialog.close());

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    /** Remoção de usuário selecionado. */
    private void onDeleteSelected() {
        AppUser selected = grid.asSingleSelect().getValue();
        if (selected == null || selected.getId() == null) {
            Notification.show("Selecione um usuário.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            userService.deleteById(selected.getId());
            Notification.show("Usuário removido com sucesso.",
                            3000, Notification.Position.MIDDLE)
                    .addThemeNames("success");
            reloadGrid();
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Erro ao remover usuário: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
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

    private static String trimOrNull(String v) {
        return v == null ? null : v.trim();
    }
}