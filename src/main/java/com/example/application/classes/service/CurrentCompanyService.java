package com.example.application.classes.service;

import com.example.application.classes.model.Company;
import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.repository.CompanyRepository;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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

    /** Sessão já tem empresa selecionada? */
    public boolean hasSelection() {
        return holder().isSelected();
    }

    /** Id da empresa ativa na sessão, ou lança se não houver. */
    public long activeCompanyIdOrThrow() {
        if (!holder().isSelected()) {
            throw new IllegalStateException("Nenhuma empresa selecionada para esta sessão.");
        }
        return holder().getCompanyId();
    }

    /** Nome da empresa ativa (ou null). */
    public String activeCompanyNameOrNull() {
        return holder().getCompanyName();
    }

    /** True se o vínculo atual é admin. */
    public boolean isAdmin() {
        return holder().isSelected() && holder().isAdmin();
    }

    /** Limpa seleção da sessão. */
    public void clearSelection() {
        holder().clear();
    }

    /** Faz a seleção de uma empresa do usuário, validando vínculo ativo. */
    @Transactional(readOnly = true)
    public void selectCompanyForUser(long userId, long companyId) throws SQLException {
        Optional<UserCompanyLink> linkOpt = userCompanyService.findActiveLink(userId, companyId);
        if (linkOpt.isEmpty()) {
            throw new IllegalStateException("Usuário não possui vínculo ativo com esta empresa.");
        }
        UserCompanyLink link = linkOpt.get();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("Empresa não encontrada: id=" + companyId));

        holder().set(company.getId(), company.getName(), link.isAdmin());
    }

    /**
     * Se o usuário só tem 1 vínculo ativo, seleciona automaticamente.
     * @return true se selecionou, false caso contrário.
     */
    @Transactional(readOnly = true)
    public boolean ensureAutoSelectionIfSingle(long userId) throws SQLException {
        if (holder().isSelected()) return false;

        List<CompanyChoice> choices = userCompanyService.companyChoicesFor(userId);
        if (choices.size() == 1) {
            long companyId = choices.getFirst().id;
            selectCompanyForUser(userId, companyId);
            return true;
        }
        return false;
    }

    /**
     * Lista as empresas para as quais o usuário possui vínculo ativo.
     */

    @Transactional(readOnly = true)
    public List<CompanyChoice> listSelectableChoicesForUser(long userId) throws SQLException {
        return userCompanyService.companyChoicesFor(userId);
    }
}