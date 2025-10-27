package com.example.application.classes.service;

import com.example.application.classes.model.Company;
import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.repository.CompanyRepository;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Service
public class CurrentCompanyService {

    private final CompanyRepository companyRepository;
    private final UserCompanyService userCompanyService;
    private final ObjectFactory<CurrentCompanyHolder> holderFactory;

    public CurrentCompanyService(CompanyRepository companyRepository,
                                 UserCompanyService userCompanyService,
                                 ObjectFactory<CurrentCompanyHolder> holderFactory) {
        this.companyRepository = companyRepository;
        this.userCompanyService = userCompanyService;
        this.holderFactory = holderFactory;
    }

    private CurrentCompanyHolder holder() {
        return holderFactory.getObject();
    }

    public boolean hasSelection() {
        return holder().isSelected();
    }

    public long activeCompanyIdOrThrow() {
        if (!holder().isSelected()) {
            throw new IllegalStateException("Nenhuma empresa selecionada para esta sessão.");
        }
        return holder().getCompanyId();
    }

    public String activeCompanyNameOrNull() {
        return holder().getCompanyName();
    }

    @Transactional(readOnly = true)
    public void selectCompanyForUser(long userId, long companyId) throws SQLException {
        var linkOpt = userCompanyService.findActiveLink(userId, companyId);
        if (linkOpt.isEmpty()) {
            throw new IllegalStateException("Usuário não possui vínculo ativo com esta empresa.");
        }
        UserCompanyLink link = linkOpt.get();

        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("Empresa não encontrada: id=" + companyId));

        holder().set(c.getId(), c.getName(), link.isAdmin());
    }

    @Transactional(readOnly = true)
    public boolean ensureAutoSelectionIfSingle(long userId) throws SQLException {
        if (holder().isSelected()) return true;

        List<UserCompanyLink> links = userCompanyService.companiesOf(userId);
        if (links.size() == 1) {
            long companyId = links.get(0).getCompanyId();
            selectCompanyForUser(userId, companyId);
            return true;
        }
        return false;
    }

    public void clearSelection() {
        holder().clear();
    }

    public boolean isAdmin() {
        return holder().isSelected() && holder().isAdmin();
    }
}