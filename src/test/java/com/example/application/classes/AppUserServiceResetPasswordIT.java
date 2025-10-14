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
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppUserServiceResetPasswordIT {

    @Autowired
    private AppUserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppUserService service;

    @Test
    @Sql(statements = {
            "DROP TABLE IF EXISTS app_user;",
            "CREATE TABLE app_user (" +
                    "  id BIGSERIAL PRIMARY KEY," +
                    "  creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  version INT DEFAULT 0," +
                    "  email VARCHAR(255) NOT NULL," +
                    "  name VARCHAR(255)," +
                    "  password_hash VARCHAR(255)," +
                    "  prov_pw_hash VARCHAR(255)," +
                    "  email_conf_time TIMESTAMP," +
                    "  reset_token VARCHAR(100)," +
                    "  reset_token_expiry TIMESTAMP" +
                    ");",
            "CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email_lower ON app_user(LOWER(email));",
            "INSERT INTO app_user (id, email, name, password_hash, version) VALUES (1, 'teste@exemplo.com', 'Teste', '$2a$10$7rOq2b0x', 0);",
            // simular token válido para o id=1
            "UPDATE app_user SET reset_token='tokentest', reset_token_expiry=DATEADD('MINUTE', 30, CURRENT_TIMESTAMP) WHERE id=1;"
    })
    void resetPassword_integration_ok() throws SQLException {
        // sanity: confirmar que o token está lá
        assertEquals(Optional.of(1L), repo.findIdByResetToken("tokentest"));

        // executa
        String novaSenha = "SenhaMuitoForte123";
        service.resetPassword("tokentest", novaSenha);

        // token deve ter sido limpo e senha atualizada
        assertEquals(Optional.empty(), repo.findIdByResetToken("tokentest"));

        // pegar hash novo para conferir
        AppUser user = repo.findById(1L).orElseThrow();
        String novoHash = user.getPasswordHash();
        assertNotNull(novoHash);
        assertTrue(passwordEncoder.matches(novaSenha, novoHash), "hash precisa conferir com a nova senha");
    }
}