package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenServiceTest {

    @Test
    void signsShortLivedAccessTokenWithUserAndSessionClaims() {
        byte[] keyBytes = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JwtTokenService tokenService = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)), 15, 30);
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setTipoPerfil("MOTORISTA");
        UUID sessionId = UUID.randomUUID();

        var decoded = decoder.decode(tokenService.criarAccessToken(usuario, sessionId));

        assertEquals("7", decoded.getSubject());
        assertEquals("MOTORISTA", decoded.getClaimAsString("role"));
        assertEquals(sessionId.toString(), decoded.getClaimAsString("sid"));
    }
}
