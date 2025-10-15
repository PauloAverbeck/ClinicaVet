package com.example.application.classes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// cria tabela e insere o seed (id=1 com 'tokentest' válido)
@Sql(scripts={"/schema-h2.sql", "/data-h2.sql"}, executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AppUserServiceResetPasswordIT {

    @Autowired private AppUserRepository repo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AppUserService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void resetPassword_integration_ok() throws SQLException {
        assertEquals(Optional.of(1L), repo.findIdByResetToken("tokentest"));

        String novaSenha = "SenhaMuitoForte123";
        service.resetPassword("tokentest", novaSenha);

        assertEquals(Optional.empty(), repo.findIdByResetToken("tokentest"));

        AppUser user = repo.findById(1L).orElseThrow();
        assertNotNull(user.getPasswordHash());
        assertTrue(passwordEncoder.matches(novaSenha, user.getPasswordHash()));
    }

    @Test
    void resetPassword_noToken_mustFail() {
        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword("nao-existe", "SenhaForte123"));
    }

    @Test
    void resetPassword_expiredToken_mustFail() {
        // Ajusta o token do usuário 1 para expirar no passado
        jdbc.update("""
            UPDATE app_user
               SET reset_token = 'expired',
                   reset_token_expiry = DATEADD('MINUTE', -5, CURRENT_TIMESTAMP)
             WHERE id = 1
        """);

        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword("expired", "SenhaForte123"));
    }

    @Test
    void resetPassword_weakPassword_mustFail() {
        // Garante token válido para cair na validação de força de senha
        jdbc.update("""
            UPDATE app_user
               SET reset_token = 'weakpass',
                   reset_token_expiry = DATEADD('MINUTE', 30, CURRENT_TIMESTAMP)
             WHERE id = 1
        """);

        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword("weakpass", "123"));
    }

    @Test
    void resetPassword_alreadyUsedToken_mustFail() {
        // Prepara token válido
        jdbc.update("""
            UPDATE app_user
               SET reset_token = 'usedtoken',
                   reset_token_expiry = DATEADD('MINUTE', 30, CURRENT_TIMESTAMP)
             WHERE id = 1
        """);

        // 1ª chamada consome o token
        service.resetPassword("usedtoken", "SenhaMuitoForte123");

        // 2ª chamada com o MESMO token deve falhar
        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword("usedtoken", "OutraSenhaForte123"));
    }

    @Test
    void resetPassword_nullInputs_mustFail() {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword(null, "SenhaForte123"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("token", null));
    }

    @Test
    void resetPassword_emptyInputs_mustFail() {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("", "SenhaForte123"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("token", ""));
    }

    @Test
    void resetPassword_whitespaceInputs_mustFail() {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("   ", "SenhaForte123"));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("token", "   "));
    }

    @Test
    void resetPassword_concurrentRequests() throws SQLException {
        // Usa o seed 'tokentest' e consome numa chamada
        service.resetPassword("tokentest", "NovaSenha#1");
        // Segunda tentativa com o mesmo token deve falhar
        assertThrows(IllegalArgumentException.class, () ->
                service.resetPassword("tokentest", "OutraSenha#2"));

        assertTrue(repo.findById(1L).isPresent());
    }
}