package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.example.application.classes.model.AppUser;
import com.example.application.classes.service.AppUserService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;
import com.example.application.config.ViewGuard;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@PageTitle("Usuários")
@Route(value = "users", layout = MainLayout.class)
@Menu(title = "Usuários", icon = "la la-users", order = 5)
//TODO Usar @RolesAllowed quando implementar controle de acesso
public class AppUserView extends Main {

    private final AppUserService userService;
    private final UserCompanyService userCompanyService;
    private final CurrentUserService currentUserService;

    private final Grid<AppUser> grid;

    public AppUserView(final AppUserService userService,
                       final UserCompanyService userCompanyService,
                       final CurrentUserService currentUserService) {
        this.userService = Objects.requireNonNull(userService);
        this.userCompanyService = Objects.requireNonNull(userCompanyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);

        var header = new ViewToolbar("Usuários");
        add(header);

        this.grid = buildGrid();
        add(grid);

        reloadGrid();
    }

    private Grid<AppUser> buildGrid() {
        final var grid = new Grid<>(AppUser.class, false);
        grid.setWidthFull();

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

    private void reloadGrid() {
        try {
            // Carrega dados
            List<AppUser> users = userService.listAll();
            Map<Long, String> companiesMap = userCompanyService.companiesByUserIdAggregated();
            grid.setItems(users);

            // Atualiza o renderer da coluna "Empresa"
            var col = grid.getColumnByKey("companies");
            if (col != null) {
                col.setRenderer(new ComponentRenderer<Text, AppUser>(
                        u -> new Text(companiesMap.getOrDefault(u.getId(), ""))
                ));
            }

        } catch (SQLException ex) {
            Notification.show("Erro ao listar usuários: " + ex.getMessage(),
                            6000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            grid.setItems(List.of());
        }
    }

    //TODO Remover quando implementar controle de acesso
    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        ViewGuard.requireLogin(currentUserService, () -> {
            Notification.show("Faça login para continuar.", 3000, Notification.Position.MIDDLE);
            UI.getCurrent().navigate("home");
        });
    }
}