package com.example.application.classes.service;

import com.example.application.classes.model.Client;
import com.example.application.classes.repository.ClientRepository;
import com.example.application.config.ServiceGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ServiceGuard serviceGuard;

    public ClientService(ClientRepository clientRepository,
                         ServiceGuard serviceGuard) {
        this.clientRepository = clientRepository;
        this.serviceGuard = serviceGuard;
    }

    @Transactional
    public long create(Client client) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        client.setCompanyId(companyId);
        client.setCreatedByUserId(serviceGuard.requireUserId());

        validate(client);

        return clientRepository.insert(companyId, client)
                .orElseThrow(() -> new SQLException("Falha ao inserir cliente."));
    }

    @Transactional
    public void updateBasics(Client client) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        client.setCompanyId(companyId);

        validate(client);

        boolean ok = clientRepository.updateBasics(companyId, client);
        if (!ok) {
            throw new SQLException("Cliente não encontrado ou conflito de versão.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Client> findById(long id) throws SQLException {
        return clientRepository.findById(serviceGuard.requireCompanyId(), id);
    }

    @Transactional(readOnly = true)
    public List<Client> listAllForCompany() throws SQLException {
        return clientRepository.listByCompany(serviceGuard.requireCompanyId());
    }

    @Transactional
    public void softDelete(long id) throws SQLException {
        boolean ok = clientRepository.softDelete(serviceGuard.requireCompanyId(), id);
        if (!ok) {
            throw new SQLException("Cliente não encontrado ou já removido.");
        }
    }

    private void validate(Client c) throws SQLException {
    }
}