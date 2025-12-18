package com.example.application.base.ui;

import com.example.application.classes.model.AppUser;
import com.example.application.classes.service.AppUserService;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.Lumo;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@Layout
public final class MainLayout extends AppLayout implements AfterNavigationObserver, BeforeEnterObserver {

    private final CurrentUserService currentUserService;
    private final AppUserService appUserService;
    private final CurrentCompanyService currentCompanyService;

    private final DrawerToggle toggle;
    private final Div drawerHeader;
    private final Scroller drawerScroller;
    private final SideNav nav;
    private final Div centerInfo;
    private final Button logoutBtn;
    private SideNavItem manageCompanyUsersItem;


    public MainLayout(CurrentUserService currentUserService,
                      AppUserService appUserService,
                      CurrentCompanyService currentCompanyService) {
        this.currentUserService = currentUserService;
        this.appUserService = appUserService;
        this.currentCompanyService = currentCompanyService;

        setPrimarySection(Section.DRAWER);
        UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);

        // --- HEADER / NAVBAR ---

        toggle = new DrawerToggle();

        Button homeBtn = new Button(new Icon(VaadinIcon.HOME));
        homeBtn.addThemeNames("tertiary-inline");
        homeBtn.addClickListener(e -> UI.getCurrent().navigate("home"));

        logoutBtn = new Button(new Icon(VaadinIcon.SIGN_OUT));
        logoutBtn.addThemeNames("error", "tertiary-inline");
        logoutBtn.getElement().setProperty("title", "Sair");
        logoutBtn.getStyle().set("margin-left", "auto");
        logoutBtn.addClickListener(e -> {
            currentUserService.logout();
            currentCompanyService.clearSelection();
            setDrawerOpened(false);
            UI.getCurrent().navigate("home");
        });

        // container da esquerda (toggle + home)
        Div leftBox = new Div(toggle, homeBtn);
        leftBox.addClassNames(Display.FLEX, AlignItems.CENTER, Gap.SMALL);

        // container da direita (logout)
        Div rightBox = new Div(logoutBtn);
        rightBox.addClassNames(Display.FLEX, AlignItems.CENTER, Gap.SMALL);

        // label central com info de usuário/empresa
        centerInfo = new Div();
        centerInfo.addClassNames(TextColor.SECONDARY, FontSize.SMALL);
        centerInfo.getStyle().set("position", "absolute");
        centerInfo.getStyle().set("left", "50%");
        centerInfo.getStyle().set("transform", "translateX(-50%)");

        // top bar que vai na navbar
        Div topBar = new Div();
        topBar.addClassNames(Display.FLEX, AlignItems.CENTER, Width.FULL, Padding.Horizontal.MEDIUM);
        topBar.getStyle().set("position", "relative");
        topBar.add(leftBox, centerInfo);

        addToNavbar(true, topBar);
        addToNavbar(rightBox);

        // --- DRAWER ---

        drawerHeader = createDrawerHeader();
        nav = createSideNav();
        drawerScroller = new Scroller(nav);

        addToDrawer(drawerHeader, drawerScroller);

        updateForLoginState();
    }

    private Div createDrawerHeader() {
        var appLogo = VaadinIcon.MENU.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span("Menu");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }

    private SideNav createSideNav() {
        SideNav nav = new SideNav();
        nav.addClassNames(Margin.Horizontal.MEDIUM);

        SideNavItem home = new SideNavItem("Início");
        home.setPrefixComponent(VaadinIcon.HOME.create());
        home.addItem(
                new SideNavItem("Home", "home", VaadinIcon.HOME.create()),
                new SideNavItem("Criar conta", "signup", VaadinIcon.PLUS_CIRCLE.create()),
                new SideNavItem("Esqueci minha senha", "forgot", VaadinIcon.KEY.create())
        );
        nav.addItem(home);

        SideNavItem company = new SideNavItem("Empresa");
        company.setPrefixComponent(VaadinIcon.BUILDING.create());
        company.addItem(
                new SideNavItem("Empresas", "companies", VaadinIcon.BUILDING.create()),
                new SideNavItem("Nova empresa", "company/new", VaadinIcon.PLUS_CIRCLE.create()),
                new SideNavItem("Selecionar empresa", "company/select", VaadinIcon.CHECK.create())
        );
        manageCompanyUsersItem = new SideNavItem("Gerenciar usuários", "company/users", VaadinIcon.USERS.create());
        company.addItem(manageCompanyUsersItem);

        nav.addItem(company);

        SideNavItem registration = new SideNavItem("Cadastros");
        registration.setPrefixComponent(VaadinIcon.FILE_TEXT.create());
        registration.addItem(
                new SideNavItem("Usuários", "users", VaadinIcon.USER.create()),
                new SideNavItem("Clientes", "clients", VaadinIcon.USER_HEART.create()),
                new SideNavItem("Pets", "pets", VaadinIcon.PIGGY_BANK.create()),
                new SideNavItem("Agenda", "agenda", VaadinIcon.CALENDAR.create())
        );
        nav.addItem(registration);
        updateAdminVisibility();
        return nav;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        updateForLoginState();
        updateAdminVisibility();
    }

    /** Esconde/mostra navbar + drawer e info central conforme esteja logado ou não. */
    private void updateForLoginState() {
        boolean loggedIn = currentUserService.isLoggedIn();

        toggle.setVisible(loggedIn);
        drawerHeader.setVisible(loggedIn);
        drawerScroller.setVisible(loggedIn);
        logoutBtn.setVisible(loggedIn);

        if (loggedIn) {
            centerInfo.getElement().setProperty("innerHTML", buildCenterInfoText());
            centerInfo.setVisible(true);
            setDrawerOpened(true);
        } else {
            centerInfo.getElement().setProperty("innerHTML", "");
            centerInfo.setVisible(false);
        }
    }

    private void updateAdminVisibility() {
        if (manageCompanyUsersItem == null) return;
        boolean admin = currentCompanyService.hasSelection() && currentCompanyService.isAdmin();
        manageCompanyUsersItem.setVisible(admin);
    }

    private String buildCenterInfoText() {
        try {
            if (!currentUserService.isLoggedIn()) {
                return "";
            }

            long userId = currentUserService.requireUserId();
            var userOpt = appUserService.findById(userId);
            if (userOpt.isEmpty()) {
                return "";
            }
            AppUser u = userOpt.get();

            String userPart = u.getName() + " (" + u.getEmail() + ")";
            String companyName = currentCompanyService.activeCompanyNameOrNull();

            if (companyName == null || companyName.isBlank()) {
                return "Usuário: " + userPart;
            }
            return "Usuário: " + userPart + " <br>Empresa: " + companyName;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        var viewClass = beforeEnterEvent.getNavigationTarget();
        if (viewClass.isAnnotationPresent(AnonymousAllowed.class)) return;
        if (!currentUserService.isLoggedIn()) {
            beforeEnterEvent.rerouteTo("home");
        }
    }
}