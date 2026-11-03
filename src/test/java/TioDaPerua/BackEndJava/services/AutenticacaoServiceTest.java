package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Administrador;
import TioDaPerua.BackEndJava.entities.RefreshToken;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.repositories.RefreshTokenRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock private UsuarioService usuarioService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private JwtTokenService jwtTokenService;
    private AutenticacaoService autenticacaoService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        jwtTokenService = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), 15, 30);
        jwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        autenticacaoService = new AutenticacaoService(usuarioService, jwtTokenService, refreshTokenRepository);
    }

    @Test
    void issuesSignedAccessAndOpaqueRefreshTokensForAuthenticatedProfile() throws Exception {
        Usuario usuario = usuarioMotorista();
        when(usuarioService.autenticar("motorista@example.com", "senha")).thenReturn(usuario);
        when(usuarioService.logarMotorista(usuario)).thenReturn(motorista(usuario));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = autenticacaoService.autenticar("motorista@example.com", "senha");
        RefreshToken persistido = capturarRefreshToken();
        var accessToken = jwtDecoder.decode(response.accessToken());
        String hashEsperado = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(response.refreshToken().getBytes(StandardCharsets.UTF_8)));

        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.accessTokenExpiresIn());
        assertEquals("MOTORISTA", accessToken.getClaimAsString("role"));
        assertEquals("51", accessToken.getSubject());
        assertEquals(hashEsperado, persistido.getTokenHash());
        assertNotEquals(response.refreshToken(), persistido.getTokenHash());
        assertEquals(8L, response.perfil().id());
    }

    @Test
    void rotatesRefreshTokenAndRevokesThePreviousOne() {
        Usuario usuario = usuarioMotorista();
        UUID sessionId = UUID.randomUUID();
        RefreshToken anterior = new RefreshToken();
        anterior.setUsuario(usuario);
        anterior.setSessionId(sessionId);
        anterior.setTokenHash("stored-old-token-hash");
        anterior.setExpiraEm(Instant.now().plusSeconds(120));
        when(refreshTokenRepository.buscarPorHashParaAtualizacao(anyString()))
                .thenReturn(Optional.of(anterior));
        when(usuarioService.logarMotorista(usuario)).thenReturn(motorista(usuario));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = autenticacaoService.renovar("refresh-token-antigo");

        assertNotNull(anterior.getRevogadoEm());
        assertNotEquals("refresh-token-antigo", response.refreshToken());
        assertEquals(sessionId.toString(), jwtDecoder.decode(response.accessToken()).getClaimAsString("sid"));
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void revokesSessionWhenARefreshTokenIsReused() {
        RefreshToken reutilizado = new RefreshToken();
        UUID sessionId = UUID.randomUUID();
        reutilizado.setSessionId(sessionId);
        reutilizado.setRevogadoEm(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.buscarPorHashParaAtualizacao(anyString()))
                .thenReturn(Optional.of(reutilizado));

        assertThrows(UnauthorizedException.class,
                () -> autenticacaoService.renovar("refresh-token-reutilizado"));

        verify(refreshTokenRepository).revogarSessao(eq(sessionId), any(Instant.class));
    }

    @Test
    void rejectsRefreshForSuspendedAccountAndRevokesItsSessions() {
        Usuario usuario = usuarioMotorista();
        usuario.setContaSuspensa(true);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setSessionId(UUID.randomUUID());
        refreshToken.setExpiraEm(Instant.now().plusSeconds(120));
        when(refreshTokenRepository.buscarPorHashParaAtualizacao(anyString()))
                .thenReturn(Optional.of(refreshToken));

        assertThrows(UnauthorizedException.class,
                () -> autenticacaoService.renovar("refresh-token-de-conta-suspensa"));

        verify(refreshTokenRepository).revogarSessoesDoUsuario(eq(usuario.getId()), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logoutRevokesEveryRefreshTokenInTheAuthenticatedSession() {
        Usuario usuario = usuarioMotorista();
        UUID sessionId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setSessionId(sessionId);
        when(refreshTokenRepository.buscarPorHashParaAtualizacao(anyString()))
                .thenReturn(Optional.of(refreshToken));

        autenticacaoService.encerrarSessao("refresh-token", sessionId.toString(), usuario.getId());

        verify(refreshTokenRepository).revogarSessao(eq(sessionId), any(Instant.class));
    }

    @Test
    void passwordChangeRevokesAllRefreshSessionsForTheUser() {
        autenticacaoService.alterarSenha(51L, "senha atual", "Nova senha forte 123");

        verify(usuarioService).alterarSenha(51L, "senha atual", "Nova senha forte 123");
        verify(refreshTokenRepository).revogarSessoesDoUsuario(eq(51L), any(Instant.class));
    }

    @Test
    void authenticatesAnAdministratorThroughTheUnifiedLogin() {
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setEmail("admin@example.com");
        usuario.setTipoPerfil("ADMIN");
        Administrador administrador = new Administrador();
        administrador.setId(7L);
        when(usuarioService.autenticar("admin@example.com", "secret")).thenReturn(usuario);
        when(usuarioService.logarAdministrador(usuario)).thenReturn(administrador);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = autenticacaoService.autenticar("admin@example.com", "secret");

        assertEquals(7L, response.perfil().id());
        assertEquals("ADMIN", response.perfil().tipoPerfil());
        assertEquals("ADMIN", jwtDecoder.decode(response.accessToken()).getClaimAsString("role"));
    }

    private RefreshToken capturarRefreshToken() {
        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        return captor.getValue();
    }

    private Usuario usuarioMotorista() {
        Usuario usuario = new Usuario();
        usuario.setId(51L);
        usuario.setEmail("motorista@example.com");
        usuario.setSenha("hash");
        usuario.setTipoPerfil("MOTORISTA");
        return usuario;
    }

    private Motorista motorista(Usuario usuario) {
        Motorista motorista = new Motorista();
        motorista.setId(8L);
        motorista.setNome("Motorista");
        motorista.setUsuario(usuario);
        return motorista;
    }
}
