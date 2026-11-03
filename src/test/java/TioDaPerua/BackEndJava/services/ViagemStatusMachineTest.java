package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.DependenteResponsavelUsuarioProjection;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.LocalizacaoViagemAtualRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemStatusMachineTest {

    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private HistoricoViagemDiaRepository historicoViagemDiaRepository;
    @Mock private LocalizacaoViagemAtualRepository localizacaoViagemAtualRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private ViagemService service;

    @Test
    void requiresReasonToCancelAndPersistsActorAndReason() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(execucao.getViagem().getMotorista().getUsuario()));
        when(viagemDiaRepository.save(any(ViagemDia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoViagemDiaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BusinessException.class,
                () -> service.alterarStatusViagemDia(90L, "CANCELADA", " ", 100L));
        service.alterarStatusViagemDia(90L, "CANCELADA", "Veículo indisponível", 100L);

        ArgumentCaptor<TioDaPerua.BackEndJava.entities.HistoricoViagemDia> captor =
                ArgumentCaptor.forClass(TioDaPerua.BackEndJava.entities.HistoricoViagemDia.class);
        verify(historicoViagemDiaRepository).save(captor.capture());
        assertEquals(StatusViagemDia.PLANEJADA, captor.getValue().getStatusAnterior());
        assertEquals(StatusViagemDia.CANCELADA, captor.getValue().getStatusNovo());
        assertEquals(100L, captor.getValue().getAtor().getId());
        assertEquals("Veículo indisponível", captor.getValue().getMotivo());
        verify(localizacaoViagemAtualRepository).deleteByViagemDiaId(90L);
    }

    @Test
    void refusesToFinalizeUntilEveryPresenceIsDeliveredOrAbsent() {
        ViagemDia execucao = execucao(StatusViagemDia.EM_ANDAMENTO);
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setStatus(StatusViagemPresenca.EMBARCADO);
        when(viagemPresencaRepository.findByViagemDiaId(90L)).thenReturn(List.of(presenca));

        assertThrows(ConflictException.class,
                () -> service.alterarStatusViagemDia(90L, "FINALIZADA", null, 100L));
    }

    @Test
    void notifiesLinkedResponsiblesWhenExecutionIsCanceled() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setDependente(new TioDaPerua.BackEndJava.entities.Dependente(5L, null, null, null, null, null));
        when(viagemDiaRepository.findByIdForUpdate(90L)).thenReturn(Optional.of(execucao));
        when(viagemPresencaRepository.findByViagemDiaId(90L)).thenReturn(List.of(presenca));
        when(viagemDiaRepository.save(any(ViagemDia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById(100L))
                .thenReturn(Optional.of(execucao.getViagem().getMotorista().getUsuario()));
        when(historicoViagemDiaRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DependenteResponsavelUsuarioProjection vinculo = mock(DependenteResponsavelUsuarioProjection.class);
        when(vinculo.getUsuarioId()).thenReturn(200L);
        when(responsavelRepository.findUsuariosAtivosPorDependentes(List.of(5L)))
                .thenReturn(List.of(vinculo));

        service.alterarStatusViagemDia(90L, "CANCELADA", "Imprevisto", 100L);

        verify(notificacaoService).criarNotificacoesAutomaticas(
                100L, java.util.Set.of(200L),
                "Atualização da viagem", "A viagem foi cancelada.");
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
}
