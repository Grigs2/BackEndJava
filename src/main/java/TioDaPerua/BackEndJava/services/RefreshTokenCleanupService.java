package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;

    public RefreshTokenCleanupService(RefreshTokenRepository refreshTokenRepository,
                                      JwtTokenService jwtTokenService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void removerTokensExpirados() {
        refreshTokenRepository.removerTokensExpirados(jwtTokenService.now());
    }
}
