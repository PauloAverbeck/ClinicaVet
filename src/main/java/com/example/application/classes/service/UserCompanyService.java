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

    public UserCompanyService(UserCompanyRepository userCompanyRepository, AppUserRepository appUserRepository) {
        this.userCompanyRepository = userCompanyRepository;
        this.appUserRepository = appUserRepository;
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

    public boolean isAdmin(long userId, long companyId) throws SQLException {
        return userCompanyRepository.isUserAdmin(userId, companyId);
    }

    public Optional<UserCompanyLink> findActiveLink(long userId, long companyId) throws SQLException {
        return userCompanyRepository.findActive(userId, companyId);
    }

    public Map<Long, String> companiesByUserIdAggregated() throws SQLException {
        return userCompanyRepository.companiesByUserIdAggregated();
    }

    public List<CompanyChoice> companyChoicesFor(long userId) throws SQLException {
        return userCompanyRepository.listActiveCompanyChoicesByUser(userId);
    }

    public List<UserCompanyLink> membersOf(long companyId) throws SQLException {
        return userCompanyRepository.listActiveByCompany(companyId);
    }

    public List<UserCompanyLink> linksOfUser(long userId) throws SQLException {
        return userCompanyRepository.listActiveLinksByUser(userId);
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

        // 1) Delete first
        for (Long cid : toDelete) {
            userCompanyRepository.softDelete(userId, cid);
        }

        // 2) Add / Restore
        for (Long cid : toAdd) {
            userCompanyRepository.insertOrRestore(actingUserId, userId, cid, false);
        }
    }

    @Transactional(readOnly = true)
    public List<CompanyUserRow> listCompanyUsers(long companyId) throws SQLException {
        return appUserRepository.listCompanyUsers(companyId);
    }
}
