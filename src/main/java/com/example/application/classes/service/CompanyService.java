package com.example.application.classes.service;

import com.example.application.classes.DocumentType;
import com.example.application.classes.model.Company;
import com.example.application.classes.repository.CompanyRepository;
import com.example.application.config.ServiceGuard;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserCompanyService userCompanyService;
    private final ServiceGuard serviceGuard;

    public CompanyService(CompanyRepository companyRepository,
                          UserCompanyService userCompanyService,
                          ServiceGuard serviceGuard) {
        this.companyRepository = companyRepository;
        this.userCompanyService = userCompanyService;
        this.serviceGuard = serviceGuard;
    }

    @Transactional
    public long createForCurrentUser(String name, DocumentType type, String document) throws SQLException {
        long userId = serviceGuard.requireUserId();

        String n = normalizeName(name);
        String doc = normalizeDocument(type, document);
        validateCompanyInput(n, type, doc);

        if (companyRepository.existsByDocument(type, doc, null)) {
            throw new DuplicateKeyException("Documento já cadastrado para outra empresa.");
        }

        Company company = new Company()
                .setName(n)
                .setDocumentType(type)
                .setDocument(doc);

        try {
            long companyId = companyRepository.insert(company)
                    .orElseThrow(() -> new SQLException("Falha ao inserir empresa."));

            userCompanyService.linkAsAdmin(userId, userId, companyId);
            return companyId;

        } catch (SQLException ex) {
            // Postgres unique_violation = 23505
            if ("23505".equals(ex.getSQLState())
                    || String.valueOf(ex.getMessage()).toLowerCase().contains("unique")) {
                throw new DuplicateKeyException("Documento já cadastrado para outra empresa.", ex);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Company> findById(long id) throws SQLException {
        serviceGuard.requireUserId();
        return companyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Company> listAll() throws SQLException {
        serviceGuard.requireUserId();
        return companyRepository.listAll();
    }

    @Transactional(readOnly = true)
    public List<Company> searchByName(String name, int limit) throws SQLException {
        serviceGuard.requireUserId();
        return companyRepository.searchByName(name, limit);
    }

    @Transactional
    public void updateBasics(Company company) throws SQLException {

        if (company == null || company.getId() <= 0) {
            throw new IllegalArgumentException("Empresa inválida para atualização.");
        }

        serviceGuard.requireSelectedCompanyEquals(company.getId());
        serviceGuard.requireAdminOfCompany(company.getId());

        company.setName(normalizeName(company.getName()));
        company.setDocument(normalizeDocument(company.getDocumentType(), company.getDocument()));
        validateCompanyInput(company.getName(), company.getDocumentType(), company.getDocument());

        if (companyRepository.existsByDocument(company.getDocumentType(), company.getDocument(), company.getId())) {
            throw new DuplicateKeyException("Documento já cadastrado para outra empresa.");
        }

        try {
            boolean ok = companyRepository.updateBasics(company);
            if (!ok) {
                throw new SQLException("Empresa não encontrada, removida, ou conflito de versão.");
            }
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())
                    || String.valueOf(ex.getMessage()).toLowerCase().contains("unique")) {
                throw new DuplicateKeyException("Documento já cadastrado para outra empresa.", ex);
            }
            throw ex;
        }
    }

    @Transactional
    public void deleteById(long id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID inválido para remoção.");
        }

        serviceGuard.requireSelectedCompanyEquals(id);
        serviceGuard.requireAdminOfCompany(id);

        boolean ok = companyRepository.softDeleteById(id);
        if (!ok) {
            throw new SQLException("Nenhuma empresa ativa encontrada para remover. ID=" + id);
        }
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private static String normalizeDocument(DocumentType documentType, String document) {
        if (document == null) return null;
        String doc = document.trim();
        if (documentType == DocumentType.CPF || documentType == DocumentType.CNPJ) {
            doc = doc.replaceAll("\\D+", "");
        }
        if (documentType == DocumentType.PASSPORT) {
            doc = doc.toUpperCase();
        }
        return doc;
    }

    private static void validateCompanyInput(String name, DocumentType documentType, String document) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome da empresa é obrigatório.");
        }
        if (documentType == null || document == null || document.isBlank()) {
            throw new IllegalArgumentException("Tipo de documento e documento são obrigatórios.");
        }

        switch (documentType) {
            case CPF -> {
                if (!document.matches("\\d{11}")) {
                    throw new IllegalArgumentException("CPF deve conter 11 dígitos.");
                }
            }
            case CNPJ -> {
                if (!document.matches("\\d{14}")) {
                    throw new IllegalArgumentException("CNPJ deve conter 14 dígitos.");
                }
            }
            case PASSPORT -> {
                if (!document.matches("^[A-Z0-9]{5,9}$")) {
                    throw new IllegalArgumentException("Passaporte deve ter entre 5 e 9 caracteres, sem especiais.");
                }
            }
        }
    }
}