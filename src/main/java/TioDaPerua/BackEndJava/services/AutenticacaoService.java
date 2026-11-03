package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.AutenticacaoResponseDTO;
import TioDaPerua.BackEndJava.DTOs.PerfilAutenticadoDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Administrador;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.RefreshToken;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.repositories.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AutenticacaoService {

    private final UsuarioService usuarioService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AutenticacaoService(UsuarioService usuarioService,
                               JwtTokenService jwtTokenService,
                               RefreshTokenRepository refreshTokenRepository) {
        this.usuarioService = usuarioService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public AutenticacaoResponseDTO autenticar(String email, String senha) {
        Usuario usuario = usuarioService.autenticar(email, senha);
        UUID sessionId = UUID.randomUUID();
        String refreshToken = novoRefreshToken(usuario, sessionId);
        return criarResposta(usuario, sessionId, refreshToken);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AutenticacaoResponseDTO renovar(String refreshTokenRecebido) {
        if (refreshTokenRecebido == null || refreshTokenRecebido.isBlank()) {
            throw new UnauthorizedException("Refresh token inválido");
        }

        RefreshToken atual = refreshTokenRepository.buscarPorHashParaAtualizacao(
                        TokenHasher.sha256Hex(refreshTokenRecebido))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido ou expirado"));

        Instant agora = jwtTokenService.now();
        if (atual.getRevogadoEm() != null) {
            refreshTokenRepository.revogarSessao(atual.getSessionId(), agora);
            throw new UnauthorizedException("Refresh token reutilizado; a sessão foi encerrada");
        }
        if (Boolean.TRUE.equals(atual.getUsuario().getContaSuspensa())) {
            refreshTokenRepository.revogarSessoesDoUsuario(atual.getUsuario().getId(), agora);
            throw new UnauthorizedException("Refresh token inválido ou expirado");
        }
        if (!atual.getExpiraEm().isAfter(agora)) {
            throw new UnauthorizedException("Refresh token inválido ou expirado");
        }

        atual.setRevogadoEm(agora);
        refreshTokenRepository.save(atual);

        String refreshTokenNovo = novoRefreshToken(atual.getUsuario(), atual.getSessionId());
        return criarResposta(atual.getUsuario(), atual.getSessionId(), refreshTokenNovo);
    }

    @Transactional
    public void encerrarSessao(String refreshTokenRecebido, String sessionIdClaim, Long usuarioId) {
        if (refreshTokenRecebido == null || refreshTokenRecebido.isBlank()
                || sessionIdClaim == null || usuarioId == null) {
            throw new UnauthorizedException("Sessão inválida");
        }

        RefreshToken token = refreshTokenRepository.buscarPorHashParaAtualizacao(
                        TokenHasher.sha256Hex(refreshTokenRecebido))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (!token.getSessionId().toString().equals(sessionIdClaim)
                || !token.getUsuario().getId().equals(usuarioId)) {
            throw new UnauthorizedException("Refresh token não pertence à sessão autenticada");
        }

        refreshTokenRepository.revogarSessao(token.getSessionId(), jwtTokenService.now());
    }

    @Transactional
    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        usuarioService.alterarSenha(usuarioId, senhaAtual, novaSenha);
        refreshTokenRepository.revogarSessoesDoUsuario(usuarioId, jwtTokenService.now());
    }

    @Transactional
    public void definirNovaSenha(Long usuarioId, String novaSenha) {
        usuarioService.definirNovaSenha(usuarioId, novaSenha);
        refreshTokenRepository.revogarSessoesDoUsuario(usuarioId, jwtTokenService.now());
    }

    private String novoRefreshToken(Usuario usuario, UUID sessionId) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUsuario(usuario);
        refreshToken.setSessionId(sessionId);
        refreshToken.setTokenHash(TokenHasher.sha256Hex(token));
        refreshToken.setCriadoEm(jwtTokenService.now());
        refreshToken.setExpiraEm(jwtTokenService.now().plus(jwtTokenService.getRefreshTokenDuration()));
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private AutenticacaoResponseDTO criarResposta(Usuario usuario, UUID sessionId, String refreshToken) {
        PerfilAutenticadoDTO perfil = criarPerfil(usuario);
        Duration accessDuration = jwtTokenService.getAccessTokenDuration();
        Duration refreshDuration = jwtTokenService.getRefreshTokenDuration();
        return new AutenticacaoResponseDTO(
                "Bearer",
                jwtTokenService.criarAccessToken(usuario, sessionId),
                accessDuration.toSeconds(),
                refreshToken,
                refreshDuration.toSeconds(),
                perfil
        );
    }

    private PerfilAutenticadoDTO criarPerfil(Usuario usuario) {
        return switch (usuario.getTipoPerfil()) {
            case "MOTORISTA" -> {
                Motorista motorista = usuarioService.logarMotorista(usuario);
                yield new PerfilAutenticadoDTO(motorista.getId(), usuario.getId(),
                        motorista.getNome(), usuario.getTipoPerfil());
            }
            case "RESPONSAVEL" -> {
                Responsavel responsavel = usuarioService.logarResponsavel(usuario);
                yield new PerfilAutenticadoDTO(responsavel.getId(), usuario.getId(),
                        responsavel.getNome(), usuario.getTipoPerfil());
            }
            case "ESCOLA" -> {
                Escola escola = usuarioService.logarEscola(usuario);
                yield new PerfilAutenticadoDTO(escola.getId(), usuario.getId(),
                        escola.getNome(), usuario.getTipoPerfil());
            }
            case "ADMIN" -> {
                Administrador administrador = usuarioService.logarAdministrador(usuario);
                yield new PerfilAutenticadoDTO(administrador.getId(), usuario.getId(),
                        usuario.getEmail(), usuario.getTipoPerfil());
            }
            default -> throw new UnauthorizedException("Perfil sem acesso ao sistema");
        };
    }

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
}
