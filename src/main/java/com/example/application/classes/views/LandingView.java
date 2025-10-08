package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@PageTitle("ClinicaVet")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "home", layout = MainLayout.class)
@Menu(order = 0, icon = "la la-home", title = "Home")
@AnonymousAllowed
public class LandingView extends Main {
    private final ViewToolbar toolbar;

    public LandingView() {
        addClassNames(
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Width.FULL
        );

        //Toolbar
        this.toolbar = new ViewToolbar("ClinicaVet");
        add(toolbar);

        //Header
        final var header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);
        header.setAlignItems(FlexComponent.Alignment.START);
        header.add(
                new H1("Welcome to ClinicaVet"),
                new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam scelerisque aliquam odio et faucibus. Nulla rhoncus feugiat eros quis consectetur.")
        );
        header.addClassNames(
                LumoUtility.Margin.Bottom.MEDIUM
        );

        add(header);
        add(buttons());
    }

    //Buttons builders
    private HorizontalLayout buttons() {
        final var row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.addClassNames(LumoUtility.Gap.MEDIUM);

        //Button AppUserView
        Button btUsers = new Button("Users", new Icon(VaadinIcon.USERS));
        btUsers.addClickListener(e -> UI.getCurrent().navigate("AppUserView.class")); //Retirar Aspas quando estiver pronto
        btUsers.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        //Button CompanyView
        //Outros botoes...
        //Spaceholder

        //Button AnimalsView
        Button btAnimals = new Button("Animals", new Icon(VaadinIcon.PIGGY_BANK));
        btAnimals.addClickListener(e -> UI.getCurrent().navigate("AnimalsView.class")); //Retirar Aspas quando estiver pronto
        btAnimals.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        row.add(btUsers, btAnimals);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        return row;
    }
}
