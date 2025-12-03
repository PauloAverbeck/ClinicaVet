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

    public ClientService(
            ClientRepository clientRepository,
            CurrentCompanyService currentCompanyService,
            CurrentUserService currentUserService
    ) {
        this.clientRepository = clientRepository;
        this.currentCompanyService = currentCompanyService;
        this.currentUserService = currentUserService;
    }

    private long companyId() {
        return currentCompanyService.activeCompanyIdOrThrow();
    }

    /* CREATE */

    @Transactional
    public long create(Client client) throws SQLException {
        client.setCompanyId(companyId());
        if (currentUserService.isLoggedIn()) {
            client.setCreatedByUserId(currentUserService.requireUserId());
        }
        return clientRepository.insert(client);
    }

    /* READ */

    @Transactional(readOnly = true)
    public Optional<Client> findById(long id) throws SQLException {
        return clientRepository.findById(companyId(), id);
    }

    @Transactional(readOnly = true)
    public List<Client> listAllForCompany() throws SQLException {
        return clientRepository.listByCompany(companyId());
    }

    @Transactional(readOnly = true)
    public List<Client> searchByNameOrEmail(String query, int limit) throws SQLException {
        return clientRepository.searchByNameOrEmail(companyId(), query, limit);
    }

    /* UPDATE */

    @Transactional
    public void updateBasics(Client client) throws SQLException {
        client.setCompanyId(companyId());
        clientRepository.updateBasics(client);
    }

    /* DELETE (soft) */

    @Transactional
    public void softDelete(long id) throws SQLException {
        long companyId = companyId();
        clientRepository.softDelete(companyId, id);
    }
}
