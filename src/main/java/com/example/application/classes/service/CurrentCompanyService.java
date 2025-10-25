package com.example.application.classes.service;

import com.example.application.classes.CurrentCompanyHolder;
import com.example.application.classes.model.Company;
import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class CurrentCompanyService {

    private final CurrentCompanyHolder holder;
    private final UserCompanyService userCompanyService;
    private final CompanyRepository companyRepository;

    public CurrentCompanyService(CurrentCompanyHolder holder,
                                 UserCompanyService userCompanyService,
                                 CompanyRepository companyRepository) {
        this.holder = holder;
        this.userCompanyService = userCompanyService;
        this.companyRepository = companyRepository;
    }

    public long activeCompanyIdOrThrow() {
        if (holder.isEmpty()) {
            throw new IllegalStateException("Nenhuma empresa ativa selecionada.");
        }
        return holder.getCompanyId();
    }

    public Optional<Long> activeCompanyId() {
        return Optional.ofNullable(holder.getCompanyId());
    }

    public Optional<String> activeCompanyName() {
        return Optional.ofNullable(holder.getCompanyName());
    }

    public void clear() {
        holder.clear();
    }

    public List<UserCompanyLink> companiesOf(long userId) throws SQLException {
        return userCompanyService.companiesOf(userId);
    }

    public void setActiveCompany(long userId, long companyId) throws Exception {
        var linkOpt = userCompanyService.findActiveLink(userId, companyId)
                .orElseThrow(() -> new IllegalStateException("O usuário não está vinculado à empresa selecionada.")) ;

        var company = companyRepository.findById(companyId)
                .map(Company::getName)
                .orElse("(empresa não encontrada)");

        holder.setCompanyId(linkOpt.getCompanyId(), company);
    }

    public boolean isActiveCompanyAdmin(long userId) throws SQLException {
        Long companyId = holder.getCompanyId();
        if (companyId == null) return false;
        return userCompanyService.isAdmin(userId, companyId);
    }
}
