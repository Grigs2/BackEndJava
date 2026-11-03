package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.PasswordResetAttemptRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetRateLimitKeyRepository;
import TioDaPerua.BackEndJava.repositories.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetCleanupService {
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetAttemptRepository attemptRepository;
    private final PasswordResetRateLimitKeyRepository rateLimitKeyRepository;

    public PasswordResetCleanupService(PasswordResetTokenRepository tokenRepository,
                                       PasswordResetAttemptRepository attemptRepository,
                                       PasswordResetRateLimitKeyRepository rateLimitKeyRepository) {
        this.tokenRepository = tokenRepository;
        this.attemptRepository = attemptRepository;
        this.rateLimitKeyRepository = rateLimitKeyRepository;
    }

    @Scheduled(cron = "0 25 3 * * *")
    @Transactional
    public void limparRegistrosAntigos() {
        Instant agora = Instant.now();
        tokenRepository.removerTokensExpiradosOuUsados(agora);
        attemptRepository.deleteBySolicitadoEmBefore(agora.minus(Duration.ofHours(24)));
        rateLimitKeyRepository.deleteByLastSeenEmBefore(agora.minus(Duration.ofHours(24)));
    }
}
