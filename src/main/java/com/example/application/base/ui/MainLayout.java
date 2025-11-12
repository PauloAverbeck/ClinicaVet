package com.example.application.base.ui;

import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.Lumo;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@Layout
public final class MainLayout extends AppLayout implements AfterNavigationObserver {
    private final CurrentUserService currentUserService;

    private final DrawerToggle toggle;
    private final Div drawerHeader;
    private final Scroller drawerScroller;
    private final SideNav nav;

    public MainLayout(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;

        setPrimarySection(Section.DRAWER);
        UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);

        toggle = new DrawerToggle();
        addToNavbar(true, toggle);

        drawerHeader = createDrawerHeader();
        nav = createSideNav();
        drawerScroller = new Scroller(nav);

        addToDrawer(drawerHeader, drawerScroller);

        updateForLoginState();
    }

    private Div createDrawerHeader() {
        var appLogo = VaadinIcon.UMBRELLA.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span("Menu");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }

    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.addClassNames(Margin.Horizontal.MEDIUM);

        for (MenuEntry entry : MenuConfiguration.getMenuEntries()) {
            nav.addItem(createSideNavItem(entry));
        }
        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry entry) {
        return entry.icon() != null
                ? new SideNavItem(entry.title(), entry.path(), new Icon(entry.icon()))
                : new SideNavItem(entry.title(), entry.path());
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        updateForLoginState();
    }

    /** Esconde/mostra navbar + drawer conforme esteja logado ou não. */
    private void updateForLoginState() {
        boolean loggedIn = currentUserService.isLoggedIn();

        toggle.setVisible(loggedIn);
        drawerHeader.setVisible(loggedIn);
        drawerScroller.setVisible(loggedIn);
    }
}