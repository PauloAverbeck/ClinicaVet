package com.example.application.classes.service;

import com.example.application.classes.model.Pet;
import com.example.application.classes.repository.PetRepository;
import com.example.application.config.ServiceGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final ServiceGuard serviceGuard;

    public PetService(PetRepository petRepository,
                      ServiceGuard serviceGuard) {
        this.petRepository = petRepository;
        this.serviceGuard = serviceGuard;
    }

    private void validate(Pet pet) {
        pet.setName(trim(pet.getName()));
        pet.setSpecies(trim(pet.getSpecies()));
        pet.setBreed(trim(pet.getBreed()));
        pet.setNotes(trim(pet.getNotes()));

        if (pet.getClientId() <= 0) {
            throw new PetValidationException("Selecione o tutor do pet.");
        }

        if (pet.getName().isEmpty()) {
            throw new PetValidationException("Nome do pet é obrigatório.");
        }
        if (pet.getName().length() > 200) {
            throw new PetValidationException("Nome excede 200 caracteres.");
        }

        if (!pet.getSpecies().isEmpty() && pet.getSpecies().length() > 50) {
            throw new PetValidationException("Espécie excede 50 caracteres.");
        }

        if (!pet.getBreed().isEmpty() && pet.getBreed().length() > 100) {
            throw new PetValidationException("Raça excede 100 caracteres.");
        }

        if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(LocalDate.now())) {
            throw new PetValidationException("Data de nascimento não pode ser futura.");
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    @Transactional
    public long create(Pet pet) throws SQLException {
        long userId = serviceGuard.requireUserId();
        long companyId = serviceGuard.requireCompanyId();

        pet.setCompanyId(companyId);
        pet.setCreatedByUserId(userId);

        validate(pet);

        return petRepository.insert(pet)
                .orElseThrow(() -> new SQLException("Falha ao inserir pet."));
    }

    @Transactional
    public void updateBasics(Pet pet) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();

        pet.setCompanyId(companyId);
        validate(pet);

        boolean ok = petRepository.updateBasics(pet);
        if (!ok) {
            throw new SQLException("Pet não encontrado, removido, ou conflito de versão.");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Pet> findById(long id) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        return petRepository.findById(companyId, id);
    }

    @Transactional(readOnly = true)
    public List<Pet> listAllForCompany() throws SQLException {
        long companyId = serviceGuard.requireCompanyId();
        return petRepository.listByCompany(companyId);
    }

    @Transactional
    public void softDelete(long id) throws SQLException {
        long companyId = serviceGuard.requireCompanyId();

        boolean ok = petRepository.softDelete(companyId, id);
        if (!ok) {
            throw new SQLException("Pet não encontrado ou já removido. id=" + id);
        }
    }
}