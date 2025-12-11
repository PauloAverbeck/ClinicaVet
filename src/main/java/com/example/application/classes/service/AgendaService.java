package com.example.application.classes.service;

import com.example.application.classes.repository.AgendaRepository;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;
    private final CurrentCompanyService currentCompanyService;

    public AgendaService (AgendaRepository agendaRepository,
                         CurrentCompanyService currentCompanyService) {
        this.agendaRepository = agendaRepository;
        this.currentCompanyService = currentCompanyService;
    }

    public List<AgendaRow> listCurrentCompanyAgenda() throws SQLException {
        long companyId = currentCompanyService.activeCompanyIdOrThrow();
        return agendaRepository.listByCompany(companyId);
    }
}
