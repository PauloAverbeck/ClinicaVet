package com.example.application.classes.views;

import com.example.application.classes.model.Company;
import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.repository.CompanyRepository;
import com.example.application.classes.service.CurrentCompanyService;
import com.example.application.classes.service.CurrentUserService;
import com.example.application.classes.service.UserCompanyService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@PageTitle("Selecionar Empresa")
@Route("company/select")
@Menu(title = "Select Company", icon = "la la-building", order = 7)
@AnonymousAllowed
public class CompanySelectView extends VerticalLayout {

    private final CurrentCompanyService currentCompanyService;
    private final CurrentUserService currentUserService;
    private final UserCompanyService userCompanyService;
    private final CompanyRepository companyRepository;

    private final ComboBox<CompanyOption> companyBox = new ComboBox<>("Empresa");
    private final Button selectBtn = new Button("Usar esta empresa");

    public CompanySelectView(CurrentCompanyService currentCompanyService,
                             CurrentUserService currentUserService,
                             UserCompanyService userCompanyService,
                             CompanyRepository companyRepository) {
        this.currentCompanyService = currentCompanyService;
        this.currentUserService = currentUserService;
        this.userCompanyService = userCompanyService;
        this.companyRepository = companyRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H1("Selecionar empresa ativa"));

        companyBox.setWidth("420px");
        companyBox.setItemLabelGenerator(CompanyOption::label);

        selectBtn.addClickShortcut(Key.ENTER);
        selectBtn.addClickListener(e -> onSelect());

        add(companyBox, selectBtn, new Paragraph("""
            A empresa selecionada ficará ativa nesta sessão, permitindo cadastrar/editar dados
            (pessoas, animais, atendimentos) dentro do escopo dessa empresa.
        """));

        loadData();
        autoSelectIfSingle();
    }

    private void loadData() {
        try {
            long userId = currentUserService.currentUserIdOrThrow();
            List<UserCompanyLink> links = userCompanyService.companiesOf(userId);

            if (links.isEmpty()) {
                Notification.show("Você ainda não possui vínculo com nenhuma empresa.", 6000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                add(new Paragraph("Crie uma empresa em “Empresas” > “Register Company” para continuar."));
                companyBox.setItems(new ArrayList<>());
                selectBtn.setEnabled(false);
                return;
            }

            List<CompanyOption> items = new ArrayList<>();
            for (UserCompanyLink link : links) {
                companyRepository.findById(link.getCompanyId()).ifPresent(c ->
                        items.add(new CompanyOption(c, link.isAdmin())));
            }
            companyBox.setItems(items);
            if (currentCompanyService.hasSelection()) {
                Long activeId = currentCompanyService.activeCompanyIdOrThrow();
                items.stream()
                        .filter(opt -> Objects.equals(opt.company().getId(), activeId))
                        .findFirst()
                        .ifPresent(companyBox::setValue);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Notification.show("Falha ao carregar suas empresas: " + ex.getMessage(), 6000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void autoSelectIfSingle() {
        try {
            long userId = currentUserService.currentUserIdOrThrow();
            boolean selected = currentCompanyService.ensureAutoSelectionIfSingle(userId);
            if (selected) {
                Notification.show("Empresa selecionada automaticamente.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("home"));
            }
        } catch (Exception ex) {
        }
    }

    private void onSelect() {
        CompanyOption opt = companyBox.getValue();
        if (opt == null) {
            Notification.show("Escolha uma empresa.", 3000, Notification.Position.TOP_CENTER);
            return;
        }
        try {
            long userId = currentUserService.currentUserIdOrThrow();
            long companyId = opt.company().getId();

            currentCompanyService.selectCompanyForUser(userId, companyId);

            Notification.show("Empresa ativa: " + opt.company().getName(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Redireciona para a área que você preferir:
            getUI().ifPresent(ui -> ui.navigate("home"));
        } catch (SQLException | IllegalStateException ex) {
            Notification.show("Não foi possível selecionar esta empresa: " + ex.getMessage(), 6000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /* Helper local para exibir nome + badge admin */
    private record CompanyOption(Company company, boolean admin) {
        String label() {
            return company.getName() + (admin ? "  • ADMIN" : "");
        }
    }
}