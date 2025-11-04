package com.example.application.classes.service;

import com.example.application.classes.model.UserCompanyLink;
import com.example.application.classes.repository.UserCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class UserCompanyService {
    private final UserCompanyRepository userCompanyRepository;

    public UserCompanyService(UserCompanyRepository userCompanyRepository) {
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional
    public long linkAsAdmin(long createdByUserId, long userId, long companyId) throws SQLException {
        return userCompanyRepository.insert(createdByUserId, userId, companyId, true);
    }

    @Transactional
    public long linkMember(long createdByUserId, long userId, long companyId) throws SQLException {
        return userCompanyRepository.insert(createdByUserId, userId, companyId, false);
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

    public List<UserCompanyLink> companiesOf(long userId) throws SQLException {
        return userCompanyRepository.listActiveLinksByUser(userId);
    }

    public List<CompanyChoice> companyChoicesFor(long userId) throws SQLException {
        return userCompanyRepository.listActiveCompanyChoicesByUser(userId);
    }


    public List<UserCompanyLink> membersOf(long companyId) throws SQLException {
        return userCompanyRepository.listActiveByCompany(companyId);
    }
}
