package com.example.application.classes;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {
    private final AppUserRepository repo;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AppUserService(AppUserRepository repo) {
        this.repo = repo;
    }

    /* CREATE / UPDATE / DELETE */
    /**
     * Cria um usuário.
     * Observação: se passwordHash for null e houver provisória, o Repository
     * garante NOT NULL gravando a provisória também em password_hash.
     */
    public long create(String name,
                       String email,
                       String passwordHash,
                       LocalDateTime emailConfirmationTime,
                       String provisionalPasswordHash) throws SQLException {

        final String normalizedEmail = normalizeEmail(email);

        AppUser user = new AppUser()
                .setName(name)
                .setEmail(normalizedEmail)
                .setPasswordHash(passwordHash)
                .setEmailConfirmationTime(emailConfirmationTime)
                .setProvisionalPasswordHash(provisionalPasswordHash);

        return repo.insertWithProvisional(user);
    }

    /** Atualiza dados básicos (email, nome) com optimistic locking. */
    public void updateBasics(AppUser user) throws SQLException {
        user.setEmail(normalizeEmail(user.getEmail()));
        repo.updateDadosBasicos(user);
    }

    /** Salva (insert ou update) */
    public void save(AppUser user) throws SQLException {
        if (user.getId() == 0) {
            user.setEmail(normalizeEmail(user.getEmail())); // 👈 adicione isto
            repo.insertWithProvisional(user);
        } else {
            updateBasics(user);
        }
    }

    /** Hard delete por ID. */
    public void deleteById(long id) throws SQLException {
        repo.deleteById(id);
    }

    /* READ */
    public Optional<AppUser> findById(long id) throws SQLException {
        return repo.findById(id);
    }

    public Optional<AppUser> findByEmail(String email) throws SQLException {
        return repo.findByEmail(normalizeEmail(email));
    }

    public boolean existsByEmail(String email) throws SQLException {
        return repo.existsByEmail(normalizeEmail(email));
    }

    public List<AppUser> listAll() throws SQLException {
        return repo.listAll();
    }

    /* SIGNUP FLOW */
    /**
     * Autocadastro:
     * - Normaliza email
     * - Gera senha provisória e grava o hash com BCrypt em prov_pw_hash
     * - Insere usuário com a provisória (password_hash pode ficar null; o repo cuidará do NOT NULL)
     * - Retorna a senha provisória para que outra camada envie por e-mail/SMS.
     */
    public String requestSignup(String name, String email) throws SQLException {
        final String normalizedEmail = normalizeEmail(email);

        if (existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("E-mail já cadastrado.");
        }

        String provisionalPlain = generateTempPassword(10);
        String provisionalHash = passwordEncoder.encode(provisionalPlain);

        AppUser user = new AppUser()
                .setName(name)
                .setEmail(normalizedEmail)
                .setPasswordHash(null)
                .setEmailConfirmationTime(null)
                .setProvisionalPasswordHash(provisionalHash);

        repo.insertWithProvisional(user);

        return provisionalPlain;
    }

    /**
     * Confirmação do cadastro / primeiro login:
     * - Busca usuário por e-mail
     * - Compara a senha provisória informada (após aplicar o mesmo hash)
     * - Se confere, promove provisória para oficial e marca email_conf_time
     *
     * Retorna true em caso de sucesso; false se a senha provisória não confere.
     * Lança IllegalStateException se o usuário não existir ou não tiver provisória.
     */
    public boolean confirmSignup(String email, String provisionalPlainPassword) throws SQLException {
        AppUser user = repo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        if (user.getProvisionalPasswordHash() == null) {
            throw new IllegalStateException("Não há senha provisória pendente para este usuário.");
        }

        if (!passwordEncoder.matches(provisionalPlainPassword, user.getProvisionalPasswordHash())) {
            return false;
        }

        repo.promoteProvisionalToOfficial(user.getId());
        return true;
    }

    /**
     * Esqueci minha senha:
     * - Gera nova provisória
     * - Grava seu hash em prov_pw_hash e zera email_conf_time
     * - Retorna a senha provisória para envio por e-mail/SMS
     */
    public String forgotPassword(String email) throws SQLException {
        AppUser user = repo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));

        String provisionalPlain = generateTempPassword(10);
        String provisionalHash = passwordEncoder.encode(provisionalPlain);

        repo.setProvisional(user.getId(), provisionalHash);
        return provisionalPlain;
    }

    /* UTILITY */
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Gera uma senha provisória alfanumérica. */
    private static String generateTempPassword(int length) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    public boolean login(String email, String plainPassword) throws SQLException {
        AppUser u = repo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        String hash = u.getPasswordHash();
        if (hash == null) return false;
        return passwordEncoder.matches(plainPassword, hash);
    }
}

