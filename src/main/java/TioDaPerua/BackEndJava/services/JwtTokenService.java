package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration accessTokenDuration;
    private final Duration refreshTokenDuration;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${app.security.jwt.access-token-minutes:15}") long accessTokenMinutes,
                           @Value("${app.security.jwt.refresh-token-days:30}") long refreshTokenDays) {
        if (accessTokenMinutes <= 0 || refreshTokenDays <= 0) {
            throw new IllegalArgumentException("Os prazos dos tokens devem ser positivos");
        }
        this.jwtEncoder = jwtEncoder;
        this.clock = Clock.systemUTC();
        this.accessTokenDuration = Duration.ofMinutes(accessTokenMinutes);
        this.refreshTokenDuration = Duration.ofDays(refreshTokenDays);
    }

    public String criarAccessToken(Usuario usuario, UUID sessionId) {
        Instant agora = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiodaperua")
                .issuedAt(agora)
                .expiresAt(agora.plus(accessTokenDuration))
                .subject(usuario.getId().toString())
                .claim("role", usuario.getTipoPerfil())
                .claim("sid", sessionId.toString())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Duration getAccessTokenDuration() {
        return accessTokenDuration;
    }

    public Duration getRefreshTokenDuration() {
        return refreshTokenDuration;
    }

    public Instant now() {
        return clock.instant();
    }
}
