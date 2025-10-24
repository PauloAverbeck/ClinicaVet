package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.AppUser;
import com.example.application.classes.service.AppUserService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@PageTitle("Users")
@Route(value = "users", layout = MainLayout.class)
@Menu(title = "Users", icon = "la la-users", order = 5)

public class AppUserView extends Main {

    private final AppUserService userService;
    private final Grid<AppUser> grid;

    public AppUserView(final AppUserService userService) {
        this.userService = Objects.requireNonNull(userService);

        var header = new ViewToolbar("Users");
        add(header);

        this.grid = buildGrid();
        add(grid);

        reloadGrid();
    }

    private Grid<AppUser> buildGrid() {
        final var grid = new Grid<>(AppUser.class, false);
        grid.setWidthFull();

        grid.addColumn(user -> user.getId() == null ? "-" : user.getId().toString())
                .setHeader("ID").setAutoWidth(true).setSortable(true);

        grid.addColumn(AppUser::getName)
                .setHeader("Name").setAutoWidth(true).setSortable(true);

        grid.addColumn(AppUser::getEmail)
                .setHeader("Email").setSortable(true).setFlexGrow(1);

        return grid;
    }

    private void reloadGrid() {
        try {
            List<AppUser> users = userService.listAll();
            grid.setItems(users);
        } catch (SQLException ex) {
            Notification.show("Error listing users: " + ex.getMessage(), 6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            grid.setItems(List.of());
        }
    }
}