package com.example.application.classes.service;

import com.example.application.classes.model.Pet;
import com.example.application.classes.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final CurrentCompanyService currentCompanyService;

    public PetService(PetRepository petRepository, CurrentCompanyService currentCompanyService) {
        this.petRepository = petRepository;
        this.currentCompanyService = currentCompanyService;
    }

    private long companyId() {
        return currentCompanyService.activeCompanyIdOrThrow();
    }

    /* CREATE */

    @Transactional
    public long create(Pet pet) throws SQLException {
        pet.setCompanyId(companyId());
        return petRepository.insert(pet);
    }

    /* READ */

    @Transactional(readOnly = true)
    public Optional<Pet> findById(long id) throws SQLException {
        return petRepository.findById(companyId(), id);
    }

    @Transactional(readOnly = true)
    public List<Pet> listByClient(long clientId) throws SQLException {
        return petRepository.listByClient(companyId(), clientId);
    }

    @Transactional(readOnly = true)
    public List<Pet> listAllForCompany() throws SQLException {
        return petRepository.listByCompany(companyId());
    }

    @Transactional(readOnly = true)
    public List<Pet> searchByName(long clientId, String name) throws SQLException {
        return petRepository.searchByName(companyId(), clientId, name);
    }

    /* UPDATE */

    @Transactional
    public void updateBasics(Pet pet) throws SQLException {
        pet.setCompanyId(companyId());
        petRepository.updateBasics(pet);
    }

    /* DELETE (soft) */

    @Transactional
    public void softDelete(long id) throws SQLException {
        petRepository.softDelete(companyId(), id);
    }
}
