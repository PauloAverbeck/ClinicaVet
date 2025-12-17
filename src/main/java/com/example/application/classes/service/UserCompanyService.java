package com.example.application.classes.service;

import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.repository.AppUserRepository;
import com.example.application.classes.repository.UserCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserCompanyService {

    private final UserCompanyRepository userCompanyRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;

    public UserCompanyService(UserCompanyRepository userCompanyRepository,
                              AppUserRepository appUserRepository,
                              CurrentUserService currentUserService) {
        this.userCompanyRepository = userCompanyRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public long linkAsAdmin(long createdByUserId, long userId, long companyId) throws SQLException {
        return userCompanyRepository.insertOrRestore(createdByUserId, userId, companyId, true);
    }

    @Transactional
    public long linkMember(long createdByUserId, long userId, long companyId) throws SQLException {
        return userCompanyRepository.insertOrRestore(createdByUserId, userId, companyId, false);
    }

    @Transactional
    public void unlink(long userId, long companyId) throws SQLException {
        userCompanyRepository.softDelete(userId, companyId);
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(long userId, long companyId) throws SQLException {
        return userCompanyRepository.isUserAdmin(userId, companyId);
    }

    @Transactional(readOnly = true)
    public Optional<UserCompanyLink> findActiveLink(long userId, long companyId) throws SQLException {
        return userCompanyRepository.findActive(userId, companyId);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> companiesByUserIdAggregated() throws SQLException {
        return userCompanyRepository.companiesByUserIdAggregated();
    }

    @Transactional(readOnly = true)
    public List<CompanyChoice> companyChoicesFor(long userId) throws SQLException {
        return userCompanyRepository.listActiveCompanyChoicesByUser(userId);
    }

    @Transactional(readOnly = true)
    public List<UserCompanyLink> membersOf(long companyId) throws SQLException {
        return userCompanyRepository.listActiveByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<UserCompanyLink> linksOfUser(long userId) throws SQLException {
        return userCompanyRepository.listActiveLinksByUser(userId);
    }

    @Transactional
    public void setAdmin(long userId, long companyId, boolean isAdmin) throws SQLException {
        userCompanyRepository.setAdmin(userId, companyId, isAdmin);
    }

    @Transactional
    public void replaceCompaniesForUser(long actingUserId, long userId, Set<Long> newCompanyIds) throws SQLException {
        List<UserCompanyLink> existingLinks = userCompanyRepository.listActiveLinksByUser(userId);

        Set<Long> oldIds = existingLinks.stream()
                .map(UserCompanyLink::getCompanyId)
                .collect(Collectors.toSet());

        Set<Long> toAdd = new HashSet<>(newCompanyIds);
        toAdd.removeAll(oldIds);

        Set<Long> toDelete = new HashSet<>(oldIds);
        toDelete.removeAll(newCompanyIds);

        for (Long cid : toDelete) {
            userCompanyRepository.softDelete(userId, cid);
        }

        for (Long cid : toAdd) {
            userCompanyRepository.insertOrRestore(actingUserId, userId, cid, false);
        }
    }

    @Transactional(readOnly = true)
    public List<CompanyUserRow> listCompanyUsers(long companyId) throws SQLException {
        return appUserRepository.listCompanyUsers(companyId);
    }

    @Transactional
    public long addUserByEmailToCompany(String email, long companyId) throws SQLException {
        String normalized = normalizeEmail(email);

        var userOpt = appUserRepository.findByEmail(normalized);
        if (userOpt.isEmpty()) {
            throw new IllegalStateException("Usuário com e-mail " + normalized + " não encontrado.");
        }

        long userId = userOpt.get().getId();
        long actingUserId = currentUserService.requireUserId();

        return userCompanyRepository.insertOrRestore(actingUserId, userId, companyId, false);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}