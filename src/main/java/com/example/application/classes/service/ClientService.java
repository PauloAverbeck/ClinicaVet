package com.example.application.classes.service;

import com.example.application.classes.model.Client;
import com.example.application.classes.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final CurrentCompanyService currentCompanyService;
    private final CurrentUserService currentUserService;

    public ClientService(ClientRepository clientRepository,
                         CurrentCompanyService currentCompanyService, CurrentUserService currentUserService) {
        this.clientRepository = clientRepository;
        this.currentCompanyService = currentCompanyService;
        this.currentUserService = currentUserService;
    }

    private long companyId() {
        return currentCompanyService.activeCompanyIdOrThrow();
    }

    /* VALIDACOES */

    private void validate(Client c) throws SQLException {
        c.setName(trim(c.getName()));
        c.setEmail(trim(c.getEmail()));
        c.setPhone(trim(c.getPhone()));
        c.setCep(trim(c.getCep()));
        c.setUf(toUpper(trim(c.getUf())));
        c.setCity(trim(c.getCity()));
        c.setDistrict(trim(c.getDistrict()));
        c.setStreet(trim(c.getStreet()));
        c.setNumber(trim(c.getNumber()));
        c.setComplement(trim(c.getComplement()));
        c.setNotes(trim(c.getNotes()));
        if (c.getDocType() != null) {
            c.setDocType(c.getDocType().trim());
        }
        c.setDocument(trim(c.getDocument()));

        // Nome
        if (c.getName().isEmpty()) {
            throw new ClientValidationException("Nome é obrigatório.");
        }
        if (c.getName().length() > 200) {
            throw new ClientValidationException("Nome excede o limite de 200 caracteres.");
        }

        // E-mail
        if (c.getEmail().isEmpty()) {
            throw new ClientValidationException("E-mail é obrigatório.");
        }
        if (c.getEmail().length() > 320) {
            throw new ClientValidationException("E-mail excede o limite de 320 caracteres.");
        }
        if (!c.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ClientValidationException("E-mail inválido.");
        }

        // Telefone
        if (!c.getPhone().isEmpty() && !isValidPhone(c.getPhone())) {
            throw new ClientValidationException("Telefone inválido.");
        }

        // Documento (CPF / CNPJ / Passaporte)
        String docType = c.getDocType();
        String document = c.getDocument();

        if (docType != null && !docType.isBlank()) {
            if (document.isEmpty()) {
                throw new ClientValidationException("Documento é obrigatório para o tipo selecionado.");
            }

            String digits = cleanDigits(document);

            switch (docType) {
                case "CPF" -> {
                    if (!isValidCPF(digits)) {
                        throw new ClientValidationException("CPF inválido.");
                    }
                    c.setDocument(digits);
                }
                case "CNPJ" -> {
                    if (!isValidCNPJ(digits)) {
                        throw new ClientValidationException("CNPJ inválido.");
                    }
                    c.setDocument(digits);
                }
                case "Passaporte" -> {
                    if (document.length() < 4) {
                        throw new ClientValidationException("Passaporte inválido.");
                    }
                }
                default -> throw new ClientValidationException("Tipo de documento inválido.");
            }

            // Verificar duplicidade (empresa + doc_type + document)
            Long ignoreId = (c.getId() == 0 ? null : c.getId());
            boolean exists = clientRepository.existsByCompanyAndDocument(
                    companyId(), docType, c.getDocument(), ignoreId
            );
            if (exists) {
                throw new ClientValidationException("Já existe um cliente com este documento nesta empresa.");
            }
        }

        // CEP
        if (!c.getCep().isEmpty()) {
            if (!isValidCep(c.getCep())) {
                throw new ClientValidationException("CEP inválido.");
            }
            c.setCep(cleanDigits(c.getCep()));
        }

        // UF
        if (!c.getUf().isEmpty() && !isValidUF(c.getUf())) {
            throw new ClientValidationException("UF inválida.");
        }

        // Limites de tamanho básicos (endereço)
        if (c.getCity().length() > 100) {
            throw new ClientValidationException("Cidade excede 100 caracteres.");
        }
        if (c.getDistrict().length() > 100) {
            throw new ClientValidationException("Bairro excede 100 caracteres.");
        }
        if (c.getStreet().length() > 150) {
            throw new ClientValidationException("Rua excede 150 caracteres.");
        }
        if (c.getNumber().length() > 20) {
            throw new ClientValidationException("Número excede 20 caracteres.");
        }
        if (c.getComplement().length() > 150) {
            throw new ClientValidationException("Complemento excede 150 caracteres.");
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String toUpper(String s) {
        return s == null ? null : s.toUpperCase();
    }

    private String cleanDigits(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /* Telefone Brasil*/
    private boolean isValidPhone(String phone) {
        return phone.matches("^(\\+55\\s?)?(\\(?\\d{2}\\)?\\s?)?(9?\\d{4}[- ]?\\d{4})$");
    }

    /* CEP: 8 dígitos */
    private boolean isValidCep(String cep) {
        String digits = cleanDigits(cep);
        return digits.matches("^\\d{8}$");
    }

    /* UF brasileira */
    private boolean isValidUF(String uf) {
        return uf.matches("(?i)^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$");
    }

    /* CPF */
    private boolean isValidCPF(String cpf) {
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int sum = 0;
            for (int i = 0; i < 9; i++)
                sum += (cpf.charAt(i) - '0') * (10 - i);
            int d1 = 11 - (sum % 11);
            if (d1 >= 10) d1 = 0;

            sum = 0;
            for (int i = 0; i < 10; i++)
                sum += (cpf.charAt(i) - '0') * (11 - i);
            int d2 = 11 - (sum % 11);
            if (d2 >= 10) d2 = 0;

            return d1 == (cpf.charAt(9) - '0') && d2 == (cpf.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    /* CNPJ */
    private boolean isValidCNPJ(String cnpj) {
        if (!cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int[] w1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        int[] w2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};

        try {
            int sum = 0;
            for (int i = 0; i < 12; i++)
                sum += (cnpj.charAt(i) - '0') * w1[i];
            int d1 = sum % 11;
            d1 = d1 < 2 ? 0 : 11 - d1;

            sum = 0;
            for (int i = 0; i < 13; i++)
                sum += (cnpj.charAt(i) - '0') * w2[i];
            int d2 = sum % 11;
            d2 = d2 < 2 ? 0 : 11 - d2;

            return d1 == (cnpj.charAt(12) - '0') && d2 == (cnpj.charAt(13) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    /* CRUD */

    @Transactional
    public long create(Client client) throws SQLException {
        client.setCompanyId(companyId());
        if (currentUserService.isLoggedIn()) {
            client.setCreatedByUserId(currentUserService.requireUserId());
        }
        validate(client);
        return clientRepository.insert(client);
    }

    @Transactional
    public void updateBasics(Client client) throws SQLException {
        client.setCompanyId(companyId());
        validate(client);
        clientRepository.updateBasics(client);
    }

    @Transactional(readOnly = true)
    public Optional<Client> findById(long id) throws SQLException {
        return clientRepository.findById(companyId(), id);
    }

    @Transactional(readOnly = true)
    public List<Client> listAllForCompany() throws SQLException {
        return clientRepository.listByCompany(companyId());
    }

    @Transactional(readOnly = true)
    public List<Client> searchByNameOrEmail(String name, int limit) throws SQLException {
        return clientRepository.searchByNameOrEmail(companyId(), name, limit);
    }

    @Transactional
    public void softDelete(long id) throws SQLException {
        clientRepository.softDelete(companyId(), id);
    }
}