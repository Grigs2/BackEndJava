package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.entities.HistoricoOrdemParada;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @InjectMocks private ViagemService service;

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
