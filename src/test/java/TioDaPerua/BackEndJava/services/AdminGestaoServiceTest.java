package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.AuditoriaAdministrativaRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.RefreshTokenRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminGestaoServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EscolaRepository escolaRepository;
    @Mock private ViagemEscolaRepository viagemEscolaRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private AuditoriaAdministrativaRepository auditoriaRepository;
    @Mock private AuditoriaAdministrativaService auditoriaService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenService jwtTokenService;

    private AdminGestaoService service;

    @BeforeEach
    void setUp() {
        service = new AdminGestaoService(
                usuarioRepository,
                escolaRepository,
                viagemEscolaRepository,
                responsavelRepository,
                auditoriaRepository,
                auditoriaService,
                refreshTokenRepository,
                jwtTokenService);
    }

    @Test
    void suspendsAccountRevokesRefreshTokensAndAuditsTheReason() {
        Usuario alvo = usuario(20L, "MOTORISTA", false);
        Instant agora = Instant.parse("2026-10-01T10:00:00Z");
        when(usuarioRepository.bloquearAdministradores())
                .thenReturn(List.of(usuario(1L, "ADMIN", false)));
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(alvo));
        when(jwtTokenService.now()).thenReturn(agora);

        service.suspenderUsuario(1L, 20L, "Conta irregular");

        assertTrue(alvo.getContaSuspensa());
        verify(usuarioRepository).save(alvo);
        verify(refreshTokenRepository).revogarSessoesDoUsuario(20L, agora);
        verify(auditoriaService).registrarAlteracao(
                1L, "USUARIO_SUSPENSO", "USUARIO", 20L, "SUCESSO", "Conta irregular");
    }

    @Test
    void rejectsSelfSuspensionAndSuspensionOfTheLastActiveAdministrator() {
        assertThrows(AccessDeniedException.class,
                () -> service.suspenderUsuario(1L, 1L, "Motivo obrigatório"));
        verifyNoInteractions(usuarioRepository);

        Usuario ultimoAdmin = usuario(2L, "ADMIN", false);
        when(usuarioRepository.bloquearAdministradores()).thenReturn(List.of(ultimoAdmin));
        when(usuarioRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(ultimoAdmin));

        assertThrows(ConflictException.class,
                () -> service.suspenderUsuario(1L, 2L, "Motivo obrigatório"));

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(refreshTokenRepository, auditoriaService);
    }

    @Test
    void reactivatesSuspendedAccountAndAllowsAnOptionalReason() {
        Usuario alvo = usuario(20L, "RESPONSAVEL", true);
        when(usuarioRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(alvo));

        service.reativarUsuario(1L, 20L, "  Revisão concluída  ");

        assertFalse(alvo.getContaSuspensa());
        verify(usuarioRepository).save(alvo);
        verify(auditoriaService).registrarAlteracao(
                1L, "USUARIO_REATIVADO", "USUARIO", 20L, "SUCESSO", "Revisão concluída");
    }

    @Test
    void masksEmailInAdministrativeUserSearchAndUsesBoundedPagination() {
        Usuario usuario = usuario(20L, "MOTORISTA", false);
        usuario.setEmail("motorista@exemplo.com.br");
        when(usuarioRepository.pesquisarAdministrativamente(
                isNull(), isNull(), eq("motorista@exemplo.com.br"), isNull(), eq(PageRequest.of(0, 25))))
                .thenReturn(new PageImpl<>(List.of(usuario)));

        var resultado = service.pesquisarUsuarios(
                1L, null, " ", "motorista@exemplo.com.br", null, 0, 25);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("m***@e***.br", resultado.getContent().get(0).emailMascarado());
        verify(auditoriaService).registrarConsulta(1L, "USUARIOS_PESQUISADOS", "USUARIO");
    }

    @Test
    void rejectsOutOfRangePaginationBeforeQuerying() {
        assertThrows(BusinessException.class,
                () -> service.pesquisarUsuarios(1L, null, null, null, null, 0, 101));
        verifyNoInteractions(usuarioRepository, auditoriaService);
    }

    private Usuario usuario(Long id, String perfil, boolean suspensa) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoPerfil(perfil);
        usuario.setContaSuspensa(suspensa);
        return usuario;
    }
}
