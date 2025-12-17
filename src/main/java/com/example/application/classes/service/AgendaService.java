package com.example.application.classes.service;

import com.example.application.classes.repository.AgendaRepository;
import com.example.application.config.ServiceGuard;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;
    private final ServiceGuard serviceGuard;

    public AgendaService(AgendaRepository agendaRepository,
                         ServiceGuard serviceGuard) {
        this.agendaRepository = agendaRepository;
        this.serviceGuard = serviceGuard;
    }

    public List<AgendaRow> listCurrentCompanyAgenda() throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        return agendaRepository.listByCompany(companyId);
    }
}