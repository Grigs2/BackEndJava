package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.PasswordResetAttempt;
import TioDaPerua.BackEndJava.entities.PasswordResetRateLimitKey;
import TioDaPerua.BackEndJava.entities.PasswordResetToken;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.PasswordResetAttemptRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetRateLimitKeyRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetRateLimitKeyRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetTokenRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetRequestTransactionTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordResetAttemptRepository attemptRepository;
    @Mock private PasswordResetRateLimitKeyRepository rateLimitKeyRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordResetFingerprint fingerprint;

    private PasswordResetRequestTransaction transaction;

    @BeforeEach
    void setUp() {
        when(fingerprint.hash("email", "responsavel@example.com")).thenReturn("email-hash");
        when(fingerprint.hash("ip", "192.0.2.10")).thenReturn("ip-hash");
        when(rateLimitKeyRepository.bloquearChaves(any()))
                .thenReturn(List.of(new PasswordResetRateLimitKey(), new PasswordResetRateLimitKey()));
        transaction = new PasswordResetRequestTransaction(tokenRepository, attemptRepository,
                rateLimitKeyRepository, usuarioRepository, fingerprint);
    }

    @Test
    void createsOneTimeTokenAndStoresOnlyItsHash() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(51L);
        when(usuarioRepository.findByEmailIgnoreCase("responsavel@example.com")).thenReturn(Optional.of(usuario));
        ArgumentCaptor<PasswordResetAttempt> attemptCaptor = ArgumentCaptor.forClass(PasswordResetAttempt.class);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);

        Optional<PasswordResetEmailRequest> request =
                transaction.preparar("Responsavel@Example.com", "192.0.2.10");

        verify(attemptRepository).save(attemptCaptor.capture());
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetEmailRequest emailRequest = request.orElseThrow();
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(emailRequest.token().getBytes(StandardCharsets.UTF_8)));
        assertEquals("responsavel@example.com", emailRequest.email());
        assertEquals(expectedHash, tokenCaptor.getValue().getTokenHash());
        assertFalse(emailRequest.token().equals(tokenCaptor.getValue().getTokenHash()));
        assertEquals(Duration.ofMinutes(15),
                Duration.between(tokenCaptor.getValue().getCriadoEm(), tokenCaptor.getValue().getExpiraEm()));
        assertEquals("email-hash", attemptCaptor.getValue().getEmailFingerprint());
        assertEquals("ip-hash", attemptCaptor.getValue().getIpFingerprint());
    }

    @Test
    void rateLimitsRequestsBeforeLookingUpAnAccount() {
        when(attemptRepository.countByEmailFingerprintAndSolicitadoEmAfter(anyString(), any()))
                .thenReturn(5L);

        Optional<PasswordResetEmailRequest> request =
                transaction.preparar("responsavel@example.com", "192.0.2.10");

        assertFalse(request.isPresent());
        verify(attemptRepository, never()).save(any(PasswordResetAttempt.class));
        verify(usuarioRepository, never()).findByEmail(anyString());
    }

    @Test
    void recordsUnknownEmailAttemptsWithoutCreatingResetTokens() {
        when(usuarioRepository.findByEmailIgnoreCase("responsavel@example.com")).thenReturn(Optional.empty());

        Optional<PasswordResetEmailRequest> request =
                transaction.preparar("responsavel@example.com", "192.0.2.10");

        assertFalse(request.isPresent());
        verify(attemptRepository).save(any(PasswordResetAttempt.class));
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
    }
}
