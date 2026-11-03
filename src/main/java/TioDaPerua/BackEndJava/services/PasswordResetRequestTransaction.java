package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.PasswordResetAttempt;
import TioDaPerua.BackEndJava.entities.PasswordResetToken;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.PasswordResetAttemptRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetRateLimitKeyRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetTokenRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetRequestTransaction {
    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final Duration REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetAttemptRepository attemptRepository;
    private final PasswordResetRateLimitKeyRepository rateLimitKeyRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetFingerprint fingerprint;

    public PasswordResetRequestTransaction(PasswordResetTokenRepository tokenRepository,
                                           PasswordResetAttemptRepository attemptRepository,
                                           PasswordResetRateLimitKeyRepository rateLimitKeyRepository,
                                           UsuarioRepository usuarioRepository,
                                           PasswordResetFingerprint fingerprint) {
        this.tokenRepository = tokenRepository;
        this.attemptRepository = attemptRepository;
        this.rateLimitKeyRepository = rateLimitKeyRepository;
        this.usuarioRepository = usuarioRepository;
        this.fingerprint = fingerprint;
    }

    @Transactional
    public Optional<PasswordResetEmailRequest> preparar(String email, String ip) {
        if (email == null || email.isBlank() || ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("E-mail e origem da solicitação são obrigatórios");
        }
        Instant agora = Instant.now();
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        String emailFingerprint = fingerprint.hash("email", emailNormalizado);
        String ipFingerprint = fingerprint.hash("ip", ip);
        List<String> fingerprints = List.of(emailFingerprint, ipFingerprint).stream().sorted().toList();
        fingerprints.forEach(key -> rateLimitKeyRepository.registrarUso(key, agora));
        if (rateLimitKeyRepository.bloquearChaves(fingerprints).size() != fingerprints.size()) {
            throw new IllegalStateException("Não foi possível bloquear todas as chaves de limite de redefinição");
        }

        if (estaLimitado(emailFingerprint, ipFingerprint, agora)) {
            return Optional.empty();
        }

        PasswordResetAttempt tentativa = new PasswordResetAttempt();
        tentativa.setEmailFingerprint(emailFingerprint);
        tentativa.setIpFingerprint(ipFingerprint);
        tentativa.setSolicitadoEm(agora);
        attemptRepository.save(tentativa);

        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        String tokenBruto = gerarToken();
        tokenRepository.invalidarTokensAtivosDoUsuario(usuario.get().getId(), agora);
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUsuario(usuario.get());
        token.setTokenHash(TokenHasher.sha256Hex(tokenBruto));
        token.setCriadoEm(agora);
        token.setExpiraEm(agora.plus(TOKEN_VALIDITY));
        tokenRepository.save(token);
        return Optional.of(new PasswordResetEmailRequest(emailNormalizado, tokenBruto));
    }

    private boolean estaLimitado(String emailFingerprint, String ipFingerprint, Instant agora) {
        Instant inicioJanela = agora.minus(REQUEST_WINDOW);
        Instant inicioCooldown = agora.minus(REQUEST_COOLDOWN);
        return attemptRepository.countByEmailFingerprintAndSolicitadoEmAfter(
                        emailFingerprint, inicioJanela) >= MAX_REQUESTS_PER_WINDOW
                || attemptRepository.countByIpFingerprintAndSolicitadoEmAfter(
                        ipFingerprint, inicioJanela) >= MAX_REQUESTS_PER_WINDOW
                || attemptRepository.existsByEmailFingerprintAndSolicitadoEmAfter(
                        emailFingerprint, inicioCooldown)
                || attemptRepository.existsByIpFingerprintAndSolicitadoEmAfter(
                        ipFingerprint, inicioCooldown);
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
