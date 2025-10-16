package com.example.application.classes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AppUserServiceResetPasswordTest {

    private AppUserRepository repo;
    private PasswordEncoder encoder;
    private AppUserService service;
    private ResetMailer resetMailer;

    @BeforeEach
    void setUp() {
        repo = mock(AppUserRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new AppUserService(repo, encoder, resetMailer);
    }

    @Test
    void resetPassword_ok_whenTokenValidAndNotExpired() throws Exception {
        String token = "abc123";
        long userId = 42L;
        String newPlain = "NovaSenhaForte123";

        when(repo.findIdByResetToken(token)).thenReturn(Optional.of(userId));
        when(repo.getResetTokenExpiry(userId)).thenReturn(LocalDateTime.now().plusMinutes(30));

        service.resetPassword(token, newPlain);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(repo).clearTokenAndUpdatePassword(eq(userId), hashCaptor.capture());

        String encoded = hashCaptor.getValue();
        assertTrue(encoder.matches(newPlain, encoded), "hash precisa conferir com a senha nova");
    }

    @Test
    void resetPassword_throws_whenTokenInvalid() throws Exception {
        String token = "nao-existe";
        when(repo.findIdByResetToken(token)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(token, "qualquer"));
        assertTrue(ex.getMessage().toLowerCase().contains("token"));
    }

    @Test
    void resetPassword_throws_whenTokenExpired() throws Exception {
        String token = "expirado";
        long userId = 77L;

        when(repo.findIdByResetToken(token)).thenReturn(Optional.of(userId));
        when(repo.getResetTokenExpiry(userId)).thenReturn(LocalDateTime.now().minusMinutes(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(token, "qualquer"));
        assertTrue(ex.getMessage().toLowerCase().contains("expirado"));
    }
}