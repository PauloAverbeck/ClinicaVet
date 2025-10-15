package com.example.application.classes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(
        scripts = {"/schema-h2.sql", "/data-h2.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppUserServiceResetPasswordIT {

    @Autowired
    private AppUserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppUserService service;

    @Test
    void resetPassword_integration_ok() throws SQLException {
        // sanity: confirma que o token de reset existe e aponta para o usuário 1
        assertEquals(Optional.of(1L), repo.findIdByResetToken("tokentest"));

        // executa o caso de uso
        String novaSenha = "SenhaMuitoForte123";
        service.resetPassword("tokentest", novaSenha);

        // token deve ter sido removido
        assertEquals(Optional.empty(), repo.findIdByResetToken("tokentest"));

        // a senha deve ter sido atualizada (hash novo confere com a senha nova)
        AppUser user = repo.findById(1L).orElseThrow();
        String novoHash = user.getPasswordHash();
        assertNotNull(novoHash, "hash não deveria ser nulo após o reset");
        assertTrue(passwordEncoder.matches(novaSenha, novoHash), "hash precisa conferir com a nova senha");
    }
}