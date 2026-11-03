package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.CriarConviteResponsavelDTO;
import TioDaPerua.BackEndJava.entities.ConviteResponsavel;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.CategoriaVinculoResponsavel;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.ConviteResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedeResponsaveisServiceTest {

    @Mock private DependenteRepository dependenteRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ConviteResponsavelRepository conviteRepository;
    @Mock private AuditoriaAdministrativaService auditoriaService;

    private RedeResponsaveisService service;

    @BeforeEach
    void setUp() {
        service = new RedeResponsaveisService(
                dependenteRepository, responsavelRepository, usuarioRepository, conviteRepository, auditoriaService);
    }

    @Test
    void creatorCanInviteAnExistingResponsibleAccountForSevenDays() {
        Dependente dependente = dependenteComCriador();
        Responsavel criador = dependente.getResponsavelCriador();
        Responsavel convidado = responsavel(2L, usuario(20L, "RESPONSAVEL", "convidado@example.com"));
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(responsavelRepository.findByUsuarioId(10L)).thenReturn(Optional.of(criador));
        when(usuarioRepository.findByEmailIgnoreCase("convidado@example.com"))
                .thenReturn(Optional.of(convidado.getUsuario()));
        when(responsavelRepository.findByUsuarioId(20L)).thenReturn(Optional.of(convidado));
        when(conviteRepository.buscarPorDependenteEConvidadoEStatus(
                5L, 2L, StatusConviteResponsavel.PENDENTE)).thenReturn(List.of());
        when(conviteRepository.save(any(ConviteResponsavel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.convidar(5L, 10L, new CriarConviteResponsavelDTO(
                "CONVIDADO@example.com", CategoriaVinculoResponsavel.MAE));

        ArgumentCaptor<ConviteResponsavel> captor = ArgumentCaptor.forClass(ConviteResponsavel.class);
        verify(conviteRepository).save(captor.capture());
        ConviteResponsavel convite = captor.getValue();
        assertEquals(StatusConviteResponsavel.PENDENTE, convite.getStatus());
        assertEquals(CategoriaVinculoResponsavel.MAE, convite.getCategoria());
        assertEquals(Duration.ofDays(7), Duration.between(convite.getCriadoEm(), convite.getExpiraEm()));
        assertEquals("PENDENTE", result.status());
        assertEquals("convidado@example.com", result.convidadoEmail());
    }

    @Test
    void anotherLinkedGuardianCannotCreateInvitations() {
        Dependente dependente = dependenteComCriador();
        Responsavel outroResponsavel = responsavel(3L, usuario(30L, "RESPONSAVEL", "outro@example.com"));
        dependente.getResponsaveis().add(outroResponsavel);
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(responsavelRepository.findByUsuarioId(30L)).thenReturn(Optional.of(outroResponsavel));

        assertThrows(AccessDeniedException.class, () -> service.convidar(
                5L, 30L, new CriarConviteResponsavelDTO(
                        "convidado@example.com", CategoriaVinculoResponsavel.OUTRO)));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void acceptingInvitationAddsTheActiveGuardianRelationship() {
        Dependente dependente = dependenteComCriador();
        Responsavel convidado = responsavel(2L, usuario(20L, "RESPONSAVEL", "convidado@example.com"));
        ConviteResponsavel convite = convite(dependente, convidado);
        when(conviteRepository.findById(90L)).thenReturn(Optional.of(convite));
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(conviteRepository.buscarParaResposta(90L, 20L)).thenReturn(Optional.of(convite));
        when(responsavelRepository.save(convidado)).thenReturn(convidado);
        when(conviteRepository.save(convite)).thenReturn(convite);

        var result = service.responder(90L, 20L, true);

        assertEquals(StatusConviteResponsavel.ACEITO, convite.getStatus());
        assertTrue(dependente.getResponsaveis().stream()
                .anyMatch(responsavel -> responsavel.getId().equals(convidado.getId())));
        assertTrue(convidado.getDependentes().stream()
                .anyMatch(atual -> atual.getId().equals(dependente.getId())));
        assertEquals("ACEITO", result.status());
    }

    @Test
    void expiredInvitationIsMarkedExpiredWithoutAddingTheGuardian() {
        Dependente dependente = dependenteComCriador();
        Responsavel convidado = responsavel(2L, usuario(20L, "RESPONSAVEL", "convidado@example.com"));
        ConviteResponsavel convite = convite(dependente, convidado);
        convite.setExpiraEm(Instant.now().minusSeconds(1));
        when(conviteRepository.findById(90L)).thenReturn(Optional.of(convite));
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(conviteRepository.buscarParaResposta(90L, 20L)).thenReturn(Optional.of(convite));
        when(conviteRepository.save(convite)).thenReturn(convite);

        var result = service.responder(90L, 20L, true);

        assertEquals(StatusConviteResponsavel.EXPIRADO, convite.getStatus());
        assertFalse(dependente.getResponsaveis().stream()
                .anyMatch(responsavel -> responsavel.getId().equals(convidado.getId())));
        assertEquals("EXPIRADO", result.status());
    }

    @Test
    void declinesInvitationWithoutCreatingAnActiveRelationship() {
        Dependente dependente = dependenteComCriador();
        Responsavel convidado = responsavel(2L, usuario(20L, "RESPONSAVEL", "convidado@example.com"));
        ConviteResponsavel convite = convite(dependente, convidado);
        when(conviteRepository.findById(90L)).thenReturn(Optional.of(convite));
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(conviteRepository.buscarParaResposta(90L, 20L)).thenReturn(Optional.of(convite));
        when(conviteRepository.save(convite)).thenReturn(convite);

        var result = service.responder(90L, 20L, false);

        assertEquals(StatusConviteResponsavel.RECUSADO, convite.getStatus());
        assertFalse(dependente.getResponsaveis().stream()
                .anyMatch(responsavel -> responsavel.getId().equals(convidado.getId())));
        assertEquals("RECUSADO", result.status());
        verifyNoInteractions(responsavelRepository);
    }

    @Test
    void doesNotAllowDuplicatePendingInvitation() {
        Dependente dependente = dependenteComCriador();
        Responsavel criador = dependente.getResponsavelCriador();
        Responsavel convidado = responsavel(2L, usuario(20L, "RESPONSAVEL", "convidado@example.com"));
        ConviteResponsavel existente = convite(dependente, convidado);
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(responsavelRepository.findByUsuarioId(10L)).thenReturn(Optional.of(criador));
        when(usuarioRepository.findByEmailIgnoreCase("convidado@example.com"))
                .thenReturn(Optional.of(convidado.getUsuario()));
        when(responsavelRepository.findByUsuarioId(20L)).thenReturn(Optional.of(convidado));
        when(conviteRepository.buscarPorDependenteEConvidadoEStatus(
                5L, 2L, StatusConviteResponsavel.PENDENTE)).thenReturn(List.of(existente));

        assertThrows(ConflictException.class, () -> service.convidar(
                5L, 10L, new CriarConviteResponsavelDTO(
                        "convidado@example.com", CategoriaVinculoResponsavel.MAE)));
        verify(conviteRepository, never()).save(any(ConviteResponsavel.class));
    }

    @Test
    void adminCanAssignCreatorForAmbiguousLegacyDependent() {
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        Responsavel primeiro = responsavel(1L, usuario(10L, "RESPONSAVEL", "um@example.com"));
        Responsavel segundo = responsavel(2L, usuario(20L, "RESPONSAVEL", "dois@example.com"));
        dependente.setResponsaveis(new ArrayList<>(List.of(primeiro, segundo)));
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(dependenteRepository.save(dependente)).thenReturn(dependente);

        service.atribuirResponsavelCriador(5L, 2L, 99L);

        assertEquals(2L, dependente.getResponsavelCriador().getId());
        assertEquals(99L, dependente.getResponsavelCriadorDefinidoPorUsuarioId());
    }

    @Test
    void requiresAdminAssignmentWhenLegacyDependentHasMultipleResponsibleLinks() {
        Dependente dependente = new Dependente();
        Responsavel primeiro = responsavel(1L, usuario(10L, "RESPONSAVEL", "um@example.com"));
        Responsavel segundo = responsavel(2L, usuario(20L, "RESPONSAVEL", "dois@example.com"));
        dependente.setId(5L);
        dependente.setResponsaveis(new ArrayList<>(List.of(primeiro, segundo)));
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(responsavelRepository.findByUsuarioId(10L)).thenReturn(Optional.of(primeiro));

        assertThrows(BusinessException.class, () -> service.convidar(
                5L, 10L, new CriarConviteResponsavelDTO(
                        "novo@example.com", CategoriaVinculoResponsavel.OUTRO)));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void creatorCanRevokeAnAcceptedLinkAndTheHistoryIsRetained() {
        Dependente dependente = dependenteComCriador();
        Responsavel criador = dependente.getResponsavelCriador();
        Responsavel convidado = responsavel(2L, usuario(20L, "RESPONSAVEL", "convidado@example.com"));
        dependente.getResponsaveis().add(convidado);
        convidado.getDependentes().add(dependente);
        ConviteResponsavel convite = convite(dependente, convidado);
        convite.setStatus(StatusConviteResponsavel.ACEITO);
        when(dependenteRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(dependente));
        when(responsavelRepository.findByUsuarioId(10L)).thenReturn(Optional.of(criador));
        when(conviteRepository.buscarAceitosDoVinculo(
                5L, 2L, StatusConviteResponsavel.ACEITO)).thenReturn(List.of(convite));

        service.revogar(5L, 2L, 10L, false);

        assertEquals(StatusConviteResponsavel.REVOGADO, convite.getStatus());
        assertFalse(dependente.getResponsaveis().stream()
                .anyMatch(responsavel -> responsavel.getId().equals(convidado.getId())));
        assertFalse(convidado.getDependentes().stream()
                .anyMatch(atual -> atual.getId().equals(dependente.getId())));
    }

    private Dependente dependenteComCriador() {
        Responsavel criador = responsavel(1L, usuario(10L, "RESPONSAVEL", "criador@example.com"));
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        dependente.setNome("Dependente");
        dependente.setResponsavelCriador(criador);
        dependente.setResponsaveis(new ArrayList<>(List.of(criador)));
        criador.setDependentes(new ArrayList<>(List.of(dependente)));
        return dependente;
    }

    private ConviteResponsavel convite(Dependente dependente, Responsavel convidado) {
        ConviteResponsavel convite = new ConviteResponsavel();
        convite.setId(90L);
        convite.setDependente(dependente);
        convite.setConvidador(dependente.getResponsavelCriador());
        convite.setConvidado(convidado);
        convite.setCategoria(CategoriaVinculoResponsavel.PAI);
        convite.setStatus(StatusConviteResponsavel.PENDENTE);
        convite.setCriadoEm(Instant.now());
        convite.setExpiraEm(Instant.now().plus(Duration.ofDays(7)));
        return convite;
    }

    private Responsavel responsavel(Long id, Usuario usuario) {
        Responsavel responsavel = new Responsavel();
        responsavel.setId(id);
        responsavel.setNome("Responsável " + id);
        responsavel.setUsuario(usuario);
        responsavel.setDependentes(new ArrayList<>());
        return responsavel;
    }

    private Usuario usuario(Long id, String tipoPerfil, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoPerfil(tipoPerfil);
        usuario.setEmail(email);
        return usuario;
    }
}
