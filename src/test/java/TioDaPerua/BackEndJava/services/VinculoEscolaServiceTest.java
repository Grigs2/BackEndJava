package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.MotoristaBuscaDTO;
import TioDaPerua.BackEndJava.DTOs.VinculoEscolaDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.HistoricoVinculoEscola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.ViagemEscola;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoVinculoEscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VinculoEscolaServiceTest {
    private static final Long ESCOLA_ID = 10L;
    private static final Long MOTORISTA_ID = 20L;
    private static final Long ESCOLA_USUARIO_ID = 100L;
    private static final Long MOTORISTA_USUARIO_ID = 200L;

    @Mock private EscolaRepository escolaRepository;
    @Mock private MotoristaRepository motoristaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ViagemEscolaRepository vinculoRepository;
    @Mock private HistoricoVinculoEscolaRepository historicoRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private DocumentoMotoristaService documentoMotoristaService;

    private VinculoEscolaService service;
    private Escola escola;
    private Motorista motorista;
    private Usuario escolaUsuario;
    private Usuario motoristaUsuario;

    @BeforeEach
    void setUp() {
        service = new VinculoEscolaService(
                escolaRepository, motoristaRepository, usuarioRepository,
                vinculoRepository, historicoRepository,
                viagemDependenteRepository, documentoMotoristaService);
        escolaUsuario = usuario(ESCOLA_USUARIO_ID, "ESCOLA");
        motoristaUsuario = usuario(MOTORISTA_USUARIO_ID, "MOTORISTA");

        escola = new Escola();
        escola.setId(ESCOLA_ID);
        escola.setNome("Escola Exemplo");
        escola.setCnpj("11222333000181");
        escola.setUsuario(escolaUsuario);

        motorista = new Motorista();
        motorista.setId(MOTORISTA_ID);
        motorista.setNome("Motorista Exemplo");
        motorista.setCpf("52998224725");
        motorista.setUsuario(motoristaUsuario);

    }

    @Test
    void driverCanRequestAndTheInitialDecisionIsRecorded() {
        when(motoristaRepository.findByUsuarioId(MOTORISTA_USUARIO_ID)).thenReturn(Optional.of(motorista));
        when(escolaRepository.findByCnpj(escola.getCnpj())).thenReturn(Optional.of(escola));
        when(escolaRepository.findByIdForUpdate(ESCOLA_ID)).thenReturn(Optional.of(escola));
        when(usuarioRepository.findById(MOTORISTA_USUARIO_ID)).thenReturn(Optional.of(motoristaUsuario));
        when(vinculoRepository.existsByEscola_IdAndMotorista_IdAndStatusIn(
                eq(ESCOLA_ID), eq(MOTORISTA_ID), anyCollection())).thenReturn(false, false);
        stubHistoricoSave();
        doAnswer(invocation -> {
            ViagemEscola vinculo = invocation.getArgument(0);
            vinculo.setId(30L);
            return vinculo;
        }).when(vinculoRepository).save(any(ViagemEscola.class));

        VinculoEscolaDTO result = service.solicitarVinculo(
                MOTORISTA_USUARIO_ID, "11.222.333/0001-81");

        assertEquals("PENDENTE_ESCOLA", result.status());
        assertEquals("Escola Exemplo", result.escolaNome());
        verify(escolaRepository).findByIdForUpdate(ESCOLA_ID);
        ArgumentCaptor<HistoricoVinculoEscola> historicoCaptor =
                ArgumentCaptor.forClass(HistoricoVinculoEscola.class);
        verify(historicoRepository).save(historicoCaptor.capture());
        assertNull(historicoCaptor.getValue().getStatusAnterior());
        assertEquals(StatusVinculoEscola.PENDENTE_ESCOLA, historicoCaptor.getValue().getStatusNovo());
    }

    @Test
    void schoolCanFindDriverByExactCpfWithoutReturningCpfOrEmail() {
        when(escolaRepository.findByUsuarioId(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escola));
        when(motoristaRepository.findByCpf("52998224725")).thenReturn(Optional.of(motorista));

        List<MotoristaBuscaDTO> result = service.buscarMotoristas(
                ESCOLA_USUARIO_ID, null, "529.982.247-25");

        assertEquals(1, result.size());
        assertEquals(MOTORISTA_ID, result.get(0).id());
        assertEquals("Motorista Exemplo", result.get(0).nome());
        assertEquals(List.of("id", "nome"), Arrays.stream(MotoristaBuscaDTO.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList());
    }

    @Test
    void schoolInviteRequiresTheTargetMotoristToAccept() {
        when(escolaRepository.findByUsuarioId(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escola));
        when(escolaRepository.findByIdForUpdate(ESCOLA_ID)).thenReturn(Optional.of(escola));
        when(motoristaRepository.findById(MOTORISTA_ID)).thenReturn(Optional.of(motorista));
        when(usuarioRepository.findById(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escolaUsuario));
        when(vinculoRepository.existsByEscola_IdAndMotorista_IdAndStatusIn(
                eq(ESCOLA_ID), eq(MOTORISTA_ID), anyCollection())).thenReturn(false, false);
        stubHistoricoSave();
        doAnswer(invocation -> {
            ViagemEscola vinculo = invocation.getArgument(0);
            vinculo.setId(30L);
            return vinculo;
        }).when(vinculoRepository).save(any(ViagemEscola.class));

        VinculoEscolaDTO invitation = service.convidarMotorista(ESCOLA_USUARIO_ID, MOTORISTA_ID);
        ViagemEscola persisted = vinculo(invitation.id(), StatusVinculoEscola.PENDENTE_MOTORISTA);
        when(vinculoRepository.findByIdForUpdate(invitation.id())).thenReturn(Optional.of(persisted));
        when(motoristaRepository.findByUsuarioId(MOTORISTA_USUARIO_ID)).thenReturn(Optional.of(motorista));
        when(usuarioRepository.findById(MOTORISTA_USUARIO_ID)).thenReturn(Optional.of(motoristaUsuario));

        VinculoEscolaDTO accepted = service.responderComoMotorista(
                MOTORISTA_USUARIO_ID, invitation.id(), true, null);

        assertEquals("PENDENTE_MOTORISTA", invitation.status());
        assertEquals("ATIVO", accepted.status());
        verify(historicoRepository, times(2)).save(any(HistoricoVinculoEscola.class));
    }

    @Test
    void blocksDuplicatePendingRequestsForTheSamePair() {
        when(motoristaRepository.findByUsuarioId(MOTORISTA_USUARIO_ID)).thenReturn(Optional.of(motorista));
        when(escolaRepository.findByCnpj(escola.getCnpj())).thenReturn(Optional.of(escola));
        when(escolaRepository.findByIdForUpdate(ESCOLA_ID)).thenReturn(Optional.of(escola));
        when(usuarioRepository.findById(MOTORISTA_USUARIO_ID)).thenReturn(Optional.of(motoristaUsuario));
        when(vinculoRepository.existsByEscola_IdAndMotorista_IdAndStatusIn(
                eq(ESCOLA_ID), eq(MOTORISTA_ID), anyCollection())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.solicitarVinculo(MOTORISTA_USUARIO_ID, escola.getCnpj()));
        verify(vinculoRepository, never()).save(any(ViagemEscola.class));
    }

    @Test
    void refusalRequiresReasonAndDoesNotChangeTheLinkWhenMissing() {
        ViagemEscola vinculo = vinculo(30L, StatusVinculoEscola.PENDENTE_ESCOLA);
        when(vinculoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(vinculo));
        when(escolaRepository.findByUsuarioId(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escola));
        when(usuarioRepository.findById(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escolaUsuario));

        assertThrows(BusinessException.class,
                () -> service.responderComoEscola(ESCOLA_USUARIO_ID, 30L, false, "  "));

        assertEquals(StatusVinculoEscola.PENDENTE_ESCOLA, vinculo.getStatus());
        verify(vinculoRepository, never()).save(any(ViagemEscola.class));
        verify(historicoRepository, never()).save(any(HistoricoVinculoEscola.class));
    }

    @Test
    void schoolCannotDecideAnotherSchoolsRequest() {
        Escola outraEscola = new Escola();
        outraEscola.setId(11L);
        outraEscola.setUsuario(usuario(101L, "ESCOLA"));
        ViagemEscola vinculo = vinculo(30L, StatusVinculoEscola.PENDENTE_ESCOLA);
        when(vinculoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(vinculo));
        when(escolaRepository.findByUsuarioId(101L)).thenReturn(Optional.of(outraEscola));

        assertThrows(AccessDeniedException.class,
                () -> service.responderComoEscola(101L, 30L, true, null));
        verify(historicoRepository, never()).save(any(HistoricoVinculoEscola.class));
    }

    @Test
    void endingRequiresReasonAndReactivationIsLoggedAsANewTransition() {
        ViagemEscola vinculo = vinculo(30L, StatusVinculoEscola.SUSPENSO);
        when(vinculoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(vinculo));
        when(escolaRepository.findByUsuarioId(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escola));
        when(usuarioRepository.findById(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escolaUsuario));
        stubHistoricoSave();
        doAnswer(invocation -> invocation.getArgument(0))
                .when(vinculoRepository).save(any(ViagemEscola.class));

        assertThrows(BusinessException.class,
                () -> service.encerrar(ESCOLA_USUARIO_ID, 30L, null));

        VinculoEscolaDTO reativado = service.reativar(ESCOLA_USUARIO_ID, 30L, null);
        assertEquals("ATIVO", reativado.status());
        ArgumentCaptor<HistoricoVinculoEscola> historicoCaptor =
                ArgumentCaptor.forClass(HistoricoVinculoEscola.class);
        verify(historicoRepository).save(historicoCaptor.capture());
        assertEquals(StatusVinculoEscola.SUSPENSO, historicoCaptor.getValue().getStatusAnterior());
        assertEquals(StatusVinculoEscola.ATIVO, historicoCaptor.getValue().getStatusNovo());
        assertNull(historicoCaptor.getValue().getMotivo());
    }

    @Test
    void approvalIsBlockedWhileDriverDocumentsArePending() {
        ViagemEscola vinculo = vinculo(30L, StatusVinculoEscola.PENDENTE_ESCOLA);
        motorista.setVeiculo(new Veiculo());
        when(vinculoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(vinculo));
        when(escolaRepository.findByUsuarioId(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escola));
        when(usuarioRepository.findById(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escolaUsuario));
        when(documentoMotoristaService.listarPendenciasParaInicio(motorista.getId()))
                .thenReturn(List.of("Documento CNH pendente, recusado ou vencido"));

        assertThrows(BusinessException.class,
                () -> service.responderComoEscola(ESCOLA_USUARIO_ID, 30L, true, null));

        assertEquals(StatusVinculoEscola.PENDENTE_ESCOLA, vinculo.getStatus());
        verify(vinculoRepository, never()).save(any(ViagemEscola.class));
    }

    @Test
    void suspendingTheLinkEndsAgreementsWithTheSchoolStudents() {
        ViagemEscola vinculo = vinculo(30L, StatusVinculoEscola.ATIVO);
        when(vinculoRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(vinculo));
        when(escolaRepository.findByUsuarioId(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escola));
        when(usuarioRepository.findById(ESCOLA_USUARIO_ID)).thenReturn(Optional.of(escolaUsuario));
        stubHistoricoSave();
        doAnswer(invocation -> invocation.getArgument(0))
                .when(vinculoRepository).save(any(ViagemEscola.class));

        service.suspender(ESCOLA_USUARIO_ID, 30L, "Documentação vencida");

        verify(viagemDependenteRepository).desativarAcordosDoMotoristaNaEscola(motorista.getId(), escola.getId());
    }

    private ViagemEscola vinculo(Long id, StatusVinculoEscola status) {
        ViagemEscola vinculo = new ViagemEscola();
        vinculo.setId(id);
        vinculo.setEscola(escola);
        vinculo.setMotorista(motorista);
        vinculo.setCriadoPor(motoristaUsuario);
        vinculo.setCriadoEm(java.time.Instant.now());
        vinculo.setAtualizadoEm(java.time.Instant.now());
        vinculo.setStatus(status);
        return vinculo;
    }

    private Usuario usuario(Long id, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoPerfil(perfil);
        return usuario;
    }

    private void stubHistoricoSave() {
        doAnswer(invocation -> invocation.getArgument(0))
                .when(historicoRepository).save(any(HistoricoVinculoEscola.class));
    }
}
