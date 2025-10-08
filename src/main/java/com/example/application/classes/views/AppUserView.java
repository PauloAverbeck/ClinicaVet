package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.AppUser;
import com.example.application.classes.AppUserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@PageTitle("Users")
@Route(value = "users", layout = MainLayout.class)
@Menu(order = 10, icon = "la la-user", title = "Users")

public class AppUserView extends Main {

    private final AppUserService userService;
    private final ViewToolbar toolbar;
    private final Grid<AppUser> grid;
    private EmailField emailField;
    private PasswordField tempPasswordField;
    private Button btRequestSignup;
    private Button btConfirmSignup;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public AppUserView(final AppUserService userService) {
        this.userService = Objects.requireNonNull(userService);
        addClassNames(
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Width.FULL
        );

        //Toolbar
        this.toolbar = new ViewToolbar("Users");
        add(toolbar);

        //Formulario
        final var form = buildFormSection();
        add(form);

        //Grid
        this.grid = buildGrid();
        add(grid);

        reloadGrid();
        emailField.focus();
    }

    //Formulario de acoes
    private VerticalLayout buildFormSection() {
        final var vl = new VerticalLayout();
        vl.setPadding(false);
        vl.setSpacing(true);
        vl.setAlignItems(FlexComponent.Alignment.START);

        emailField = new EmailField("Email");
        emailField.setWidth("300px");
        emailField.setRequiredIndicatorVisible(true);

        tempPasswordField = new PasswordField("Temporary Password");
        tempPasswordField.setWidth("300px");

        btRequestSignup = new Button("Request Signup Password", e -> onRequestSignup());
        btRequestSignup.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btConfirmSignup = new Button("Confirm Signup", e -> onConfirmSignup());
        btConfirmSignup.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        final var actions = new HorizontalLayout(btRequestSignup, btConfirmSignup);
        actions.setSpacing(true);

        vl.add(emailField, tempPasswordField, actions);
        return vl;
    }

    //Grid e colunas
    private Grid<AppUser> buildGrid() {
        final var grid = new Grid<>(AppUser.class, false);
        grid.setWidthFull();

        grid.addColumn(u -> u.getId() == null
                        ? "-"
                        : u.getId().toString())
                .setHeader("ID")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(AppUser::getName)
                .setHeader("Name")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(AppUser::getEmail)
                .setHeader("Email")
                .setFlexGrow(1)
                .setSortable(true);

        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().getEmail() != null) {
                emailField.setValue(e.getValue().getEmail());
            }
        });

        return grid;
    }

    //Handler: Request Signup
    private void onRequestSignup() {
        try {
            final var email = emailField.getValue();
            if (email == null || email.isBlank()) {
                notifyWarn("Please provide a valid email.");
                return;
            }
            userService.requestSignup(email.trim());
            notifyOk("Signup email requested. Please check the your email for the temporary password.");
            reloadGrid();
        } catch (Exception ex) {
            notifyError(ex.getMessage());
        }
    }

    //Handler: Confirm Signup
    private void onConfirmSignup() {
        try {
            final var email = emailField.getValue();
            final var tempPassword = tempPasswordField.getValue();
            if (email == null || email.isBlank()) {
                notifyWarn("Please provide a valid email.");
                return;
            }
            if (tempPassword == null || tempPassword.isBlank()) {
                notifyWarn("Please provide the temporary password sent to your email.");
                return;
            }
            userService.confirmSignup(email.trim(), tempPassword);
            notifyOk("Signup confirmed. You can now log in with your email and the password you chose.");
            emailField.clear();
            tempPasswordField.clear();
            reloadGrid();
        } catch (Exception ex) {
            notifyError(ex.getMessage());
        }
    }

    private void reloadGrid() {
        try {
            List<AppUser> users = userService.listAll();
            grid.setItems(users);
        } catch (SQLException ex) {
            notifyError("Error listing users: " + ex.getMessage());
            grid.setItems(List.of());
        }
    }

    private void notifyOk(String msg) {
        Notification.show(msg, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void notifyWarn(String msg) {
        Notification.show(msg, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void notifyError(String msg) {
        Notification.show("Erro: " + msg, 4000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}