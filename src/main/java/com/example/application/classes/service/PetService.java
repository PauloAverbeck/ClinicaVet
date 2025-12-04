package com.example.application.classes.service;

import com.example.application.classes.model.Pet;
import com.example.application.classes.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
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

    /* VALIDAÇÃO */

    private void validate(Pet pet) {

        // Normalização
        pet.setName(trim(pet.getName()));
        pet.setSpecies(trim(pet.getSpecies()));
        pet.setBreed(trim(pet.getBreed()));
        pet.setNotes(trim(pet.getNotes()));

        // Nome
        if (pet.getName().isEmpty()) {
            throw new PetValidationException("Nome do pet é obrigatório.");
        }
        if (pet.getName().length() > 200) {
            throw new PetValidationException("Nome excede 200 caracteres.");
        }

        // Tutor
        if (pet.getClientId() == 0) {
            throw new PetValidationException("Selecione o tutor do pet.");
        }

        // Espécie
        if (pet.getSpecies().length() > 50) {
            throw new PetValidationException("Espécie excede 50 caracteres.");
        }

        // Raça
        if (pet.getBreed().length() > 100) {
            throw new PetValidationException("Raça excede 100 caracteres.");
        }

        // Nascimento
        if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(LocalDate.now().atStartOfDay())) {
            throw new PetValidationException("Data de nascimento não pode ser futura.");
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /* CRUD */

    @Transactional
    public long create(Pet pet) throws SQLException {
        pet.setCompanyId(companyId());
        validate(pet);
        return petRepository.insert(pet);
    }

    @Transactional
    public void updateBasics(Pet pet) throws SQLException {
        pet.setCompanyId(companyId());
        validate(pet);
        petRepository.updateBasics(pet);
    }

    @Transactional(readOnly = true)
    public Optional<Pet> findById(long id) throws SQLException {
        return petRepository.findById(companyId(), id);
    }

    @Transactional(readOnly = true)
    public List<Pet> listAllForCompany() throws SQLException {
        return petRepository.listByCompany(companyId());
    }

    @Transactional
    public void softDelete(long id) throws SQLException {
        petRepository.softDelete(companyId(), id);
    }
}
