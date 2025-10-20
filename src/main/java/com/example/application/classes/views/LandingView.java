package com.example.application.classes.views;

import com.example.application.base.ui.MainLayout;
import com.example.application.base.ui.component.CenteredBody;
import com.example.application.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
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
public class LandingView extends VerticalLayout {

    public LandingView() {
        addClassNames(
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Width.FULL
        );

        var header = new ViewToolbar("ClinicaVet");
        add(header);

        var body = new CenteredBody();
        add(body);
        setFlexGrow(1, body);

        var content = body.wrapper();

        content.add(
                new H1("Welcome to ClinicaVet"),
                new Paragraph("<-- Navegue pelo menu lateral!"),
                new Paragraph(("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."))
        );
        content.addClassNames(
                LumoUtility.Margin.Bottom.MEDIUM
        );
    }
}
