package com.example.application.classes.service;

import com.example.application.classes.ResetMailer;
import com.example.application.classes.model.AppUser;
import com.example.application.classes.repository.AppUserRepository;
import com.example.application.config.ServiceGuard;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final ResetMailer resetMailer;
    private final CurrentUserService currentUserService;

    private final ServiceGuard serviceGuard;

    public AppUserService(AppUserRepository repo,
                          PasswordEncoder passwordEncoder,
                          ResetMailer resetMailer,
                          CurrentUserService currentUserService,
                          ServiceGuard serviceGuard) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.resetMailer = resetMailer;
        this.currentUserService = currentUserService;
        this.serviceGuard = serviceGuard;
    }

    public enum LoginResult {
        LOGGED_IN,
        INVALID
    }

    /**
     * Tenta realizar login ou confirmar cadastro caso exista senha provisória.
     * - Se senha provisória confere: promove senha, marca email_conf_time e loga o usuário.
     * - Se senha oficial confere: loga o usuário.
     * - Caso contrário: retorna INVALID.
     */
    @Transactional
    public LoginResult loginOrConfirm(String email, String plainPassword) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        String plain = plainPassword == null ? null : plainPassword.trim();

        if (normalizedEmail == null || normalizedEmail.isBlank()) return LoginResult.INVALID;
        if (plain == null || plain.isBlank()) return LoginResult.INVALID;

        AppUser user = repo.findByEmail(normalizedEmail).orElse(null);
        if (user == null) return LoginResult.INVALID;

        String prov = user.getProvisionalPasswordHash();
        if (prov != null && passwordEncoder.matches(plain, prov)) {
            boolean promoted = repo.promoteProvisionalToOfficial(user.getId());
            if (!promoted) return LoginResult.INVALID;

            currentUserService.onLogin(user.getId(), user.getEmail());
            return LoginResult.LOGGED_IN;
        }

        String official = user.getPasswordHash();
        if (official != null && passwordEncoder.matches(plain, official)) {
            currentUserService.onLogin(user.getId(), user.getEmail());
            return LoginResult.LOGGED_IN;
        }

        return LoginResult.INVALID;
    }

    public List<AppUser> listAll() throws SQLException {
        serviceGuard.requireAdmin();
        return repo.listAll();
    }

    public Optional<AppUser> findById(long id) throws SQLException {
        serviceGuard.requireAdmin();
        return repo.findById(id);
    }

    public Optional<AppUser> findByEmail(String email) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) return Optional.empty();
        return repo.findByEmail(normalizedEmail);
    }

    @Transactional
    public void updateBasics(AppUser user) throws SQLException {
        serviceGuard.requireAdmin();
        if (user == null) throw new IllegalArgumentException("Usuário inválido.");
        user.setEmail(normalizeEmail(user.getEmail()));
        repo.updateDadosBasicos(user);
    }

    @Transactional
    public void deleteById(long id) throws SQLException {
        serviceGuard.requireAdmin();
        repo.deleteById(id);
    }

    @Transactional
    public void save(AppUser user) throws SQLException {
        serviceGuard.requireAdmin();
        if (user == null) throw new IllegalArgumentException("Usuário inválido.");
        user.setEmail(normalizeEmail(user.getEmail()));
        repo.save(user);
    }

    @Transactional
    public long create(String name,
                       String email,
                       String passwordHash,
                       LocalDateTime emailConfirmationTime,
                       String provisionalPasswordHash) throws SQLException {
        serviceGuard.requireAdmin();

        final String normalizedEmail = normalizeEmail(email);
        AppUser user = new AppUser()
                .setName(name)
                .setEmail(normalizedEmail)
                .setPasswordHash(passwordHash)
                .setEmailConfirmationTime(emailConfirmationTime)
                .setProvisionalPasswordHash(provisionalPasswordHash);

        return repo.insertWithProvisional(user);
    }

    /**
     * Autocadastro:
     * - Normaliza email
     * - Gera senha provisória e grava o hash em prov_pw_hash (BCrypt)
     * - Insere usuário com a provisória
     * - Envia senha provisória por email
     */
    @Transactional
    public void requestSignup(String name, String email) throws SQLException {
        String normalizedEmail = normalizeEmail(email);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("E-mail inválido.");
        }

        if (repo.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("E-mail já cadastrado.");
        }

        String provisionalPlain = generateTempPassword(10);
        String provisionalHash = passwordEncoder.encode(provisionalPlain);

        AppUser user = new AppUser()
                .setName(name.trim())
                .setEmail(normalizedEmail)
                .setPasswordHash(null)
                .setEmailConfirmationTime(null)
                .setProvisionalPasswordHash(provisionalHash);

        repo.insertWithProvisional(user);
        resetMailer.sendProvisionalPassword(normalizedEmail, provisionalPlain, "signup");
    }

    /**
     * Confirmação do cadastro / primeiro login:
     * - Busca usuário por e-mail
     * - Compara a senha provisória informada
     * - Se confere, promove provisória para oficial e marca email_conf_time
     */
    @Transactional
    public boolean confirmSignup(String email, String provisionalPlainPassword) throws SQLException {
        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        if (provisionalPlainPassword == null || provisionalPlainPassword.isBlank()) {
            throw new IllegalArgumentException("Senha provisória inválida.");
        }

        AppUser user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        String provHash = user.getProvisionalPasswordHash();
        if (provHash == null) {
            throw new IllegalStateException("Não há senha provisória pendente para este usuário.");
        }

        if (!passwordEncoder.matches(provisionalPlainPassword.trim(), provHash)) {
            return false;
        }

        return repo.promoteProvisionalToOfficial(user.getId());
    }

    /**
     * Define nova senha oficial após confirmação de cadastro.
     */
    @Transactional
    public void setPasswordAfterConfirm(String email, String newPassword) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        ensureStrongPassword(newPassword);

        AppUser user = repo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        String encoded = passwordEncoder.encode(newPassword.trim());
        repo.updateOfficialPasswordAndClearProvisional(user.getId(), encoded);
    }

    /**
     * Fluxo "Esqueci minha senha":
     * - Se email existir, gera nova senha provisória, grava o hash e envia por email.
     * - Retorna true se o email existia, false caso contrário.
     */
    @Transactional
    public boolean forgotPassword(String email) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) return false;

        var userOpt = repo.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) return false;

        var user = userOpt.get();

        String provisionalPlain = generateTempPassword(10);
        String provisionalHash = passwordEncoder.encode(provisionalPlain);

        repo.setProvisional(user.getId(), provisionalHash);
        resetMailer.sendProvisionalPassword(user.getEmail(), provisionalPlain, "forgot");

        return true;
    }

    private static void ensureStrongPassword(String pwd) {
        if (pwd == null || pwd.isBlank()) {
            throw new IllegalArgumentException("Senha inválida.");
        }
        String p = pwd.trim();
        boolean lenOK = p.length() >= 8;
        boolean up = p.chars().anyMatch(Character::isUpperCase);
        boolean low = p.chars().anyMatch(Character::isLowerCase);
        boolean dig = p.chars().anyMatch(Character::isDigit);

        if (!(lenOK && up && low && dig)) {
            throw new IllegalArgumentException("Senha fraca. Use 8+ caracteres com maiúscula, minúscula e número.");
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Gera senha provisória alfanumérica, evitando caracteres ambíguos. */
    private static String generateTempPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}