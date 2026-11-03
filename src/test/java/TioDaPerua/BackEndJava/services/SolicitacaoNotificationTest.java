package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoNotificationTest {

    @Mock private SolicitacaoRepository solicitacaoRepository;
    @Mock private DependenteRepository dependenteRepository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private SolicitacaoService service;

    @Test
    void notifiesTheRequestingDriverWhenTheResponsibleAccepts() {
        Solicitacao solicitacao = solicitacao();
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(solicitacao));
        when(solicitacaoRepository.save(solicitacao)).thenReturn(solicitacao);
        when(viagemDependenteRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Solicitacao resposta = service.responderSolicitacao(5L, true, 10L);

        assertTrue(resposta.isAceito());
        verify(viagemDependenteRepository).save(org.mockito.ArgumentMatchers.any());
        verify(notificacaoService).criarNotificacoesAutomaticas(
                10L, List.of(20L), "Solicitação aceita", "Sua solicitação de transporte foi aceita.");
    }

    @Test
    void notifiesTheRequestingDriverWhenTheResponsibleDeclines() {
        Solicitacao solicitacao = solicitacao();
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(solicitacao));
        when(solicitacaoRepository.save(solicitacao)).thenReturn(solicitacao);

        service.responderSolicitacao(5L, false, 10L);

        verify(viagemDependenteRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(notificacaoService).criarNotificacoesAutomaticas(
                10L, List.of(20L), "Solicitação recusada", "Sua solicitação de transporte foi recusada.");
    }

    @Test
    void doesNotSendAnotherNotificationForAnAlreadyAnsweredRequest() {
        Solicitacao solicitacao = solicitacao();
        solicitacao.setRespondido(true);
        solicitacao.setAceito(true);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(solicitacao));

        assertThrows(ConflictException.class, () -> service.responderSolicitacao(5L, true, 10L));

        verifyNoInteractions(notificacaoService);
    }

    private Solicitacao solicitacao() {
        Usuario responsavelUsuario = usuario(10L);
        Responsavel responsavel = new Responsavel();
        responsavel.setUsuario(responsavelUsuario);
        Usuario motoristaUsuario = usuario(20L);
        Motorista motorista = new Motorista();
        motorista.setUsuario(motoristaUsuario);
        Viagem viagem = new Viagem();
        viagem.setMotorista(motorista);
        Dependente dependente = new Dependente();
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.setId(5L);
        solicitacao.setResponsavel(responsavel);
        solicitacao.setViagem(viagem);
        solicitacao.setDependente(dependente);
        return solicitacao;
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
}
