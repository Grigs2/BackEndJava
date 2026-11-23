package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemPresencaRepository;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.HistoricoOrdemParada;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusSolicitacao;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemParada;
import TioDaPerua.BackEndJava.entities.ViagemParadaParticipante;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.enums.TipoOperacaoParada;
import TioDaPerua.BackEndJava.enums.TipoParada;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.repositories.AusenciaInformadaRepository;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoOrdemParadaRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemParadaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemRotaTest {

    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemParadaRepository viagemParadaRepository;
    @Mock private HistoricoOrdemParadaRepository historicoOrdemParadaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private SolicitacaoRepository solicitacaoRepository;
    @Mock private HistoricoViagemPresencaRepository historicoViagemPresencaRepository;
    @Mock private AusenciaInformadaRepository ausenciaInformadaRepository;
    @Mock private EscolaRepository escolaRepository;
    @Mock private ViagemEscolaRepository viagemEscolaRepository;
    @InjectMocks private ViagemService service;

    @Test
    void reorderingKeepsHiddenStopInItsSlotAndLeavesItUnchanged() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        List<ViagemParada> paradas = paradas(execucao);
        paradas.get(1).setRemovidaPorEscola(true);
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenAnswer(invocation ->
                paradas.stream().sorted(Comparator.comparingInt(ViagemParada::getOrdem)).toList());
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(execucao.getViagem().getMotorista().getUsuario()));

        service.reordenarParadas(90L, List.of(101L, 103L, 104L), 100L);

        ArgumentCaptor<List<HistoricoOrdemParada>> captor = ArgumentCaptor.forClass(List.class);
        verify(historicoOrdemParadaRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
        assertEquals(2, paradas.get(1).getOrdem());
        assertEquals(3, paradas.get(2).getOrdem());
    }

    @Test
    void reordersOnlyExistingStopsAndPersistsAuditForChangedStops() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        List<ViagemParada> paradas = paradas(execucao);
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenAnswer(invocation ->
                paradas.stream().sorted(Comparator.comparingInt(ViagemParada::getOrdem)).toList());
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(execucao.getViagem().getMotorista().getUsuario()));

        service.reordenarParadas(90L, List.of(101L, 103L, 102L, 104L), 100L);

        ArgumentCaptor<List<HistoricoOrdemParada>> captor = ArgumentCaptor.forClass(List.class);
        verify(historicoOrdemParadaRepository).saveAll(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().size());
        org.junit.jupiter.api.Assertions.assertEquals(3, paradas.get(1).getOrdem());
        org.junit.jupiter.api.Assertions.assertEquals(2, paradas.get(2).getOrdem());
    }

    @Test
    void keepsCompletedStopsInPlaceDuringExecution() {
        ViagemDia execucao = execucao(StatusViagemDia.EM_ANDAMENTO);
        List<ViagemParada> paradas = paradas(execucao);
        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setStatus(StatusViagemPresenca.EMBARCADO);
        ViagemParadaParticipante participante = new ViagemParadaParticipante();
        participante.setPresenca(presenca);
        paradas.get(1).setOperacao(TipoOperacaoParada.EMBARQUE);
        paradas.get(1).setParticipantes(new ArrayList<>(List.of(participante)));
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenReturn(paradas);

        assertThrows(BusinessException.class,
                () -> service.reordenarParadas(90L, List.of(101L, 103L, 102L, 104L), 100L));
    }

    @Test
    void removingLastSchoolDeactivatesTripAndRemovesItsStudentsFromPlannedExecution() {
        Escola escola = escola(7L);
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        Viagem viagem = execucao.getViagem();
        viagem.setId(70L);
        viagem.setAtivo(true);
        viagem.setEscolas(new ArrayList<>(List.of(escola)));
        List<ViagemParada> paradas = paradas(execucao);
        ViagemPresenca presenca = presenca(501L, escola);
        paradas.get(1).getParticipantes().add(participante(presenca));
        paradas.get(2).getParticipantes().add(participante(presenca));
        when(viagemRepository.findAtivasByMotoristaId(200L)).thenReturn(List.of(viagem));
        when(viagemRepository.findById(70L)).thenReturn(Optional.of(viagem));
        when(viagemDiaRepository.findByViagemIdAndStatusAndDataGreaterThanEqual(
                eq(70L), eq(StatusViagemDia.PLANEJADA), any(LocalDate.class))).thenReturn(List.of(execucao));
        when(viagemPresencaRepository.findByViagemDiaId(90L)).thenReturn(List.of(presenca));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenAnswer(invocation ->
                paradas.stream().sorted(Comparator.comparingInt(ViagemParada::getOrdem)).toList());

        service.removerEscolaDoMotorista(200L, 7L, execucao.getViagem().getMotorista().getUsuario());

        assertFalse(viagem.getAtivo());
        assertTrue(viagem.getEscolas().isEmpty());
        verify(viagemPresencaRepository).delete(presenca);
        verify(viagemDependenteRepository).desativarPorViagemId(70L);
        verify(solicitacaoRepository).encerrarPorViagemId(eq(70L), any());
    }

    @Test
    void removingSchoolRenumbersStopsAndKeepsEmptyStopWithOrderHistory() {
        Escola escolaRemovida = escola(7L);
        Escola escolaMantida = escola(8L);
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        Viagem viagem = execucao.getViagem();
        viagem.setId(70L);
        viagem.setAtivo(true);
        viagem.setEscolas(new ArrayList<>(List.of(escolaRemovida, escolaMantida)));
        List<ViagemParada> paradas = paradas(execucao);
        ViagemPresenca presenca = presenca(501L, escolaRemovida);
        paradas.get(1).getParticipantes().add(participante(presenca));
        ViagemParada paradaComHistorico = paradas.get(2);
        when(viagemRepository.findAtivasByMotoristaId(200L)).thenReturn(List.of(viagem));
        when(viagemDiaRepository.findByViagemIdAndStatusAndDataGreaterThanEqual(
                eq(70L), eq(StatusViagemDia.PLANEJADA), any(LocalDate.class))).thenReturn(List.of(execucao));
        when(viagemPresencaRepository.findByViagemDiaId(90L)).thenReturn(List.of(presenca));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenAnswer(invocation ->
                paradas.stream().sorted(Comparator.comparingInt(ViagemParada::getOrdem)).toList());
        when(historicoOrdemParadaRepository.existsByParadaId(anyLong()))
                .thenAnswer(invocation -> invocation.getArgument(0).equals(paradaComHistorico.getId()));

        service.removerEscolaDoMotorista(200L, 7L, execucao.getViagem().getMotorista().getUsuario());

        ArgumentCaptor<List<ViagemParada>> removidas = ArgumentCaptor.forClass(List.class);
        verify(viagemParadaRepository).deleteAll(removidas.capture());
        assertEquals(1, removidas.getValue().size());
        assertEquals(102L, removidas.getValue().get(0).getId());
        assertEquals(1, paradas.get(0).getOrdem());
        assertEquals(2, paradaComHistorico.getOrdem());
        assertEquals(3, paradas.get(3).getOrdem());
        assertTrue(paradaComHistorico.isRemovidaPorEscola());
        assertTrue(viagem.getAtivo());
        assertEquals(List.of(escolaMantida), viagem.getEscolas());
        verify(viagemRepository).save(viagem);
    }

    @Test
    void removingSchoolCancelsOpenRequestsOfItsStudentsAndNotifiesResponsavel() {
        Escola removida = escola(7L);
        Escola mantida = escola(8L);
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        Viagem viagem = execucao.getViagem();
        viagem.setId(70L);
        viagem.setAtivo(true);
        viagem.setEscolas(new ArrayList<>(List.of(removida, mantida)));

        Usuario usuarioResponsavel = new Usuario();
        usuarioResponsavel.setId(300L);
        Responsavel responsavel = new Responsavel();
        responsavel.setUsuario(usuarioResponsavel);
        Dependente aluno = new Dependente();
        aluno.setId(9L);
        aluno.setNome("Enzo");
        aluno.setEscola(removida);
        Solicitacao pedido = new Solicitacao();
        pedido.setId(11L);
        pedido.setRespondido(false);
        pedido.setResponsavel(responsavel);
        pedido.setDependente(aluno);

        when(viagemRepository.findAtivasByMotoristaId(200L)).thenReturn(List.of(viagem));
        when(solicitacaoRepository.findAbertasPorViagemEEscola(70L, 7L)).thenReturn(List.of(pedido));

        service.removerEscolaDoMotorista(200L, 7L, execucao.getViagem().getMotorista().getUsuario());

        assertTrue(pedido.isRespondido());
        assertFalse(pedido.isAceito());
        assertEquals(StatusSolicitacao.RECUSADA, pedido.getStatus());
        verify(solicitacaoRepository).save(pedido);
        verify(notificacaoService).criarNotificacoesAutomaticas(any(), eq(List.of(300L)), any(), any(), any());
        assertTrue(viagem.getAtivo());
    }

    @Test
    void addingSchoolWithoutActiveLinkIsRejected() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        Viagem viagem = execucao.getViagem();
        viagem.setId(70L);
        viagem.setAtivo(true);
        viagem.setEscolas(new ArrayList<>());
        viagem.getMotorista().setId(200L);
        when(viagemRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(viagem));
        when(escolaRepository.findAllById(List.of(7L))).thenReturn(List.of(escola(7L)));
        when(viagemEscolaRepository.listarIdsEscolasAtivasDoMotorista(
                200L, List.of(7L), StatusVinculoEscola.ATIVO)).thenReturn(List.of());

        assertThrows(ConflictException.class, () -> service.adicionarEscolas(70L, List.of(7L)));
        assertTrue(viagem.getEscolas().isEmpty());
        verify(viagemRepository, never()).save(any());
    }

    @Test
    void cancelledExecutionOfTodayDoesNotBlockAddingSchool() {
        ViagemDia execucao = execucao(StatusViagemDia.CANCELADA);
        Viagem viagem = execucao.getViagem();
        viagem.setId(70L);
        viagem.setAtivo(true);
        viagem.setEscolas(new ArrayList<>());
        viagem.getMotorista().setId(200L);
        when(viagemRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(viagem));
        when(viagemDiaRepository.findByViagemIdAndData(eq(70L), any(LocalDate.class)))
                .thenReturn(Optional.of(execucao));
        when(escolaRepository.findAllById(List.of(7L))).thenReturn(List.of(escola(7L)));
        when(viagemEscolaRepository.listarIdsEscolasAtivasDoMotorista(
                200L, List.of(7L), StatusVinculoEscola.ATIVO)).thenReturn(List.of(7L));
        when(viagemRepository.save(viagem)).thenReturn(viagem);

        service.adicionarEscolas(70L, List.of(7L));

        assertEquals(1, viagem.getEscolas().size());
        verify(viagemRepository).save(viagem);
    }

    private Escola escola(Long id) {
        Escola escola = new Escola();
        escola.setId(id);
        return escola;
    }

    private ViagemPresenca presenca(Long id, Escola escola) {
        Dependente dependente = new Dependente();
        dependente.setEscola(escola);
        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setId(id);
        presenca.setDependente(dependente);
        presenca.setStatus(StatusViagemPresenca.ESPERANDO);
        return presenca;
    }

    private ViagemParadaParticipante participante(ViagemPresenca presenca) {
        ViagemParadaParticipante participante = new ViagemParadaParticipante();
        participante.setPresenca(presenca);
        return participante;
    }

    private ViagemDia execucao(StatusViagemDia status) {
        Usuario usuario = new Usuario();
        usuario.setId(100L);
        Motorista motorista = new Motorista();
        motorista.setUsuario(usuario);
        Viagem viagem = new Viagem();
        viagem.setMotorista(motorista);
        ViagemDia execucao = new ViagemDia();
        execucao.setId(90L);
        execucao.setViagem(viagem);
        execucao.setStatus(status);
        return execucao;
    }

    private List<ViagemParada> paradas(ViagemDia execucao) {
        List<ViagemParada> paradas = new ArrayList<>();
        paradas.add(parada(101L, 1, TipoParada.MOTORISTA, execucao));
        paradas.add(parada(102L, 2, TipoParada.RESPONSAVEL, execucao));
        paradas.add(parada(103L, 3, TipoParada.ESCOLA, execucao));
        paradas.add(parada(104L, 4, TipoParada.MOTORISTA, execucao));
        return paradas;
    }

    private ViagemParada parada(Long id, int ordem, TipoParada tipo, ViagemDia execucao) {
        ViagemParada parada = new ViagemParada();
        parada.setId(id);
        parada.setOrdem(ordem);
        parada.setTipoParada(tipo);
        parada.setViagemDia(execucao);
        parada.setParticipantes(new ArrayList<>());
        return parada;
    }
}
