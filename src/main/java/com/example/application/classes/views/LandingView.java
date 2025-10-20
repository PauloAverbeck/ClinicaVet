package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
@Menu(title = "Home", icon = "la la-home", order = 0)
@AnonymousAllowed
public class LandingView extends Main {

    public LandingView() {
        addClassNames(
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Width.FULL
        );

        //Toolbar
        ViewToolbar toolbar = new ViewToolbar("ClinicaVet");
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
    }
}
