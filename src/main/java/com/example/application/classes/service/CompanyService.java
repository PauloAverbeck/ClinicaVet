package com.example.application.classes.service;

import com.example.application.classes.DocumentType;
import com.example.application.classes.model.Company;
import com.example.application.classes.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserCompanyService userCompanyService;

    public CompanyService(CompanyRepository companyRepository, UserCompanyService userCompanyService) {
        this.companyRepository = companyRepository;
        this.userCompanyService = userCompanyService;
    }

    /* CREATE */
    @Transactional
    public Company create(String name, DocumentType type, String document) throws SQLException {
        name = normalizeName(name);
        document = normalizeDocument(type, document);
        validateCompanyInput(name, type, document);

        if (companyRepository.existsByDocument(type, document)) {
            throw new IllegalStateException("Documento já cadastrado.");
        }

        Company company = new Company()
                .setName(name)
                .setDocumentType(type)
                .setDocument(document);

        long id = companyRepository.insert(company);
        company.setId(id);

        return company;
    }

    @Transactional
    public long createForUser(long userId, String name, DocumentType type, String document) throws SQLException {
        Company company = create(name, type, document);

        boolean isFirst = userCompanyService.linksOfUser(userId).isEmpty();
        if (isFirst) {
            userCompanyService.linkAsAdmin(userId, userId, company.getId());
        } else {
            userCompanyService.linkMember(userId, userId, company.getId());
        }

        return company.getId();
    }

    /* READ */

    public Optional<Company> findById(long id) throws SQLException {
        return companyRepository.findById(id);
    }

    public Optional<Company> findByDocument(DocumentType documentType, String document) throws SQLException {
        return companyRepository.findByDocument(documentType, document);
    }

    public boolean existsByDocument(DocumentType documentType, String document) throws SQLException {
        return companyRepository.existsByDocument(documentType, document);
    }

    public List<Company> listAll() throws SQLException {
        return companyRepository.listAll();
    }

    public List<Company> searchByName(String name, int limit) throws SQLException {
        return companyRepository.searchByName(name, limit);
    }

    /* UPDATE */

    @Transactional
    public void updateBasics(Company company) throws SQLException {
        if (company == null || company.getId() == 0) {
            throw new IllegalArgumentException("Empresa inválida para atualização.");
        }
        company.setName(normalizeName(company.getName()));
        company.setDocument(normalizeDocument(company.getDocumentType(), company.getDocument()));
        validateCompanyInput(company.getName(), company.getDocumentType(), company.getDocument());

        companyRepository.updateBasics(company);
    }

    /* DELETE */

    @Transactional
    public void deleteById(long id) throws SQLException {
        companyRepository.deleteById(id);
    }

    /* HELPERS */
    private static String normalizeName(String name) {
        return name != null ? name.trim() : null;
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
                    throw new IllegalArgumentException("Passaporte deve ter entre 5 e 9 caracteres, e não deve possuir caracteres especiais.");
                }
            }
        }
    }
}
