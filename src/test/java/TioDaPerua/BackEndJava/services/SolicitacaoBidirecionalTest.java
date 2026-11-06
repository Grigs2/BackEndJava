package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.enums.OrigemSolicitacao;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.exceptions.ConflictException;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoBidirecionalTest {

    @Mock private SolicitacaoRepository solicitacaoRepository;
    @Mock private DependenteRepository dependenteRepository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private NotificacaoService notificacaoService;
    @Mock private ResponsavelRepository responsavelRepository;
    @InjectMocks private SolicitacaoService service;

    private static final long USUARIO_RESPONSAVEL = 10L;
    private static final long USUARIO_MOTORISTA = 20L;

    @Test
    void responsavelPedeVagaENotificaOMotorista() {
        Responsavel responsavel = responsavel();
        Escola escola = escola(1L, "EMEF Pedro II");
        Dependente dependente = dependente(responsavel, escola);
        Viagem viagem = viagem(escola);
        when(responsavelRepository.findByUsuarioId(USUARIO_RESPONSAVEL)).thenReturn(Optional.of(responsavel));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(dependente));
        when(viagemRepository.findById(3L)).thenReturn(Optional.of(viagem));
        when(solicitacaoRepository.findPendenteByDependenteAndViagem(7L, 3L)).thenReturn(Optional.empty());
        when(viagemDependenteRepository.findByViagemIdAndDependenteId(3L, 7L)).thenReturn(Optional.empty());
        when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Solicitacao s = service.solicitarPeloResponsavel(3L, 7L, USUARIO_RESPONSAVEL);

        assertEquals(OrigemSolicitacao.RESPONSAVEL, s.getOrigem());
        assertEquals(responsavel, s.getResponsavel());
        verify(notificacaoService).criarNotificacoesAutomaticas(
                org.mockito.ArgumentMatchers.eq(USUARIO_RESPONSAVEL), org.mockito.ArgumentMatchers.eq(List.of(USUARIO_MOTORISTA)), org.mockito.ArgumentMatchers.eq("Pedido de vaga"), org.mockito.ArgumentMatchers.eq("Ana Souza pediu vaga para Enzo. Responda em Vínculos."),
                org.mockito.ArgumentMatchers.argThat((NotificacaoService.Contexto c) -> c.tipo() == TioDaPerua.BackEndJava.enums.TipoNotificacao.SOLICITACAO));
    }

    @Test
    void recusaPedidoParaViagemDeOutraEscola() {
        Responsavel responsavel = responsavel();
        Dependente dependente = dependente(responsavel, escola(1L, "EMEF Pedro II"));
        Viagem viagem = viagem(escola(2L, "Colégio Sol"));
        when(responsavelRepository.findByUsuarioId(USUARIO_RESPONSAVEL)).thenReturn(Optional.of(responsavel));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(dependente));
        when(viagemRepository.findById(3L)).thenReturn(Optional.of(viagem));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.solicitarPeloResponsavel(3L, 7L, USUARIO_RESPONSAVEL));
        assertTrue(e.getMessage().contains("Colégio Sol"));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void responsavelNaoPedeVagaParaAlunoDeOutraPessoa() {
        Responsavel responsavel = responsavel();
        Responsavel outro = new Responsavel();
        outro.setId(99L);
        Dependente dependente = dependente(outro, escola(1L, "EMEF Pedro II"));
        when(responsavelRepository.findByUsuarioId(USUARIO_RESPONSAVEL)).thenReturn(Optional.of(responsavel));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(dependente));

        assertThrows(AccessDeniedException.class, () -> service.solicitarPeloResponsavel(3L, 7L, USUARIO_RESPONSAVEL));
    }

    @Test
    void naoPedeVagaParaAlunoQueJaEstaNaViagem() {
        Responsavel responsavel = responsavel();
        Escola escola = escola(1L, "EMEF Pedro II");
        Dependente dependente = dependente(responsavel, escola);
        Viagem viagem = viagem(escola);
        ViagemDependente vinculo = new ViagemDependente();
        vinculo.setAtivo(true);
        when(responsavelRepository.findByUsuarioId(USUARIO_RESPONSAVEL)).thenReturn(Optional.of(responsavel));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(dependente));
        when(viagemRepository.findById(3L)).thenReturn(Optional.of(viagem));
        when(solicitacaoRepository.findPendenteByDependenteAndViagem(7L, 3L)).thenReturn(Optional.empty());
        when(viagemDependenteRepository.findByViagemIdAndDependenteId(3L, 7L)).thenReturn(Optional.of(vinculo));

        assertThrows(ConflictException.class, () -> service.solicitarPeloResponsavel(3L, 7L, USUARIO_RESPONSAVEL));
    }

    @Test
    void motoristaAceitaPedidoDoResponsavelEIncluiOAluno() {
        Solicitacao s = solicitacaoAberta(OrigemSolicitacao.RESPONSAVEL);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(s));
        when(solicitacaoRepository.save(s)).thenReturn(s);

        Solicitacao resposta = service.responderComoMotorista(5L, true, USUARIO_MOTORISTA);

        assertTrue(resposta.isRespondido());
        assertTrue(resposta.isAceito());
        verify(viagemDependenteRepository).save(any());
        verify(notificacaoService).criarNotificacoesAutomaticas(
                org.mockito.ArgumentMatchers.eq(USUARIO_MOTORISTA), org.mockito.ArgumentMatchers.eq(List.of(USUARIO_RESPONSAVEL)), org.mockito.ArgumentMatchers.eq("Pedido de vaga aceito"), org.mockito.ArgumentMatchers.eq("Seu pedido de vaga para Enzo foi aceito."),
                org.mockito.ArgumentMatchers.argThat((NotificacaoService.Contexto c) -> c.tipo() == TioDaPerua.BackEndJava.enums.TipoNotificacao.SOLICITACAO));
    }

    @Test
    void motoristaNaoRespondeOProprioConvite() {
        Solicitacao s = solicitacaoAberta(OrigemSolicitacao.MOTORISTA);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(s));

        assertThrows(BusinessException.class, () -> service.responderComoMotorista(5L, true, USUARIO_MOTORISTA));
        verify(viagemDependenteRepository, never()).save(any());
    }

    @Test
    void outroMotoristaNaoRespondePedido() {
        Solicitacao s = solicitacaoAberta(OrigemSolicitacao.RESPONSAVEL);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(s));

        assertThrows(AccessDeniedException.class, () -> service.responderComoMotorista(5L, true, 77L));
    }

    @Test
    void responsavelNaoRespondeOProprioPedido() {
        Solicitacao s = solicitacaoAberta(OrigemSolicitacao.RESPONSAVEL);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(s));

        assertThrows(BusinessException.class, () -> service.responderSolicitacao(5L, true, USUARIO_RESPONSAVEL));
        verify(viagemDependenteRepository, never()).save(any());
    }

    @Test
    void conviteDoMotoristaRegistraOrigemENotificaOResponsavel() {
        Responsavel responsavel = responsavel();
        Escola escola = escola(1L, "EMEF Pedro II");
        Dependente dependente = dependente(responsavel, escola);
        Viagem viagem = viagem(escola);
        when(viagemRepository.findById(3L)).thenReturn(Optional.of(viagem));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(dependente));
        when(solicitacaoRepository.findPendenteByDependenteAndViagem(7L, 3L)).thenReturn(Optional.empty());
        when(viagemDependenteRepository.findByViagemIdAndDependenteId(3L, 7L)).thenReturn(Optional.empty());
        when(solicitacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Solicitacao s = service.solicitarDependente(3L, 7L, USUARIO_MOTORISTA);

        assertEquals(OrigemSolicitacao.MOTORISTA, s.getOrigem());
        verify(notificacaoService).criarNotificacoesAutomaticas(
                org.mockito.ArgumentMatchers.eq(USUARIO_MOTORISTA), org.mockito.ArgumentMatchers.eq(List.of(USUARIO_RESPONSAVEL)), org.mockito.ArgumentMatchers.eq("Convite de transporte"), org.mockito.ArgumentMatchers.eq("Você recebeu um convite para incluir Enzo em uma viagem. Responda em Vínculos."),
                org.mockito.ArgumentMatchers.argThat((NotificacaoService.Contexto c) -> c.tipo() == TioDaPerua.BackEndJava.enums.TipoNotificacao.SOLICITACAO));
    }

    @Test
    void alunosDisponiveisFiltramPorEscolaDaViagemENomeSemAcento() {
        Escola escola = escola(1L, "EMEF Pedro II");
        Viagem viagem = viagem(escola);
        Dependente enzo = dependente(responsavel(), escola);
        Dependente joao = dependente(responsavel(), escola);
        joao.setNome("João Pedro");
        Dependente outraEscola = dependente(responsavel(), escola(2L, "Colégio Sol"));
        outraEscola.setNome("Joana");
        when(viagemRepository.findById(3L)).thenReturn(Optional.of(viagem));
        when(dependenteRepository.findDisponiveisParaViagem(3L)).thenReturn(List.of(enzo, joao, outraEscola));

        assertEquals(List.of(enzo, joao), service.listarDependentesDisponiveis(3L, null));
        assertEquals(List.of(joao), service.listarDependentesDisponiveis(3L, "joao"));
    }

    private Solicitacao solicitacaoAberta(OrigemSolicitacao origem) {
        Responsavel responsavel = responsavel();
        Escola escola = escola(1L, "EMEF Pedro II");
        Solicitacao s = new Solicitacao();
        s.setId(5L);
        s.setResponsavel(responsavel);
        s.setDependente(dependente(responsavel, escola));
        s.setViagem(viagem(escola));
        s.setOrigem(origem);
        return s;
    }

    private Responsavel responsavel() {
        Responsavel r = new Responsavel();
        r.setId(1L);
        r.setNome("Ana Souza");
        r.setUsuario(usuario(USUARIO_RESPONSAVEL));
        return r;
    }

    private Dependente dependente(Responsavel responsavel, Escola escola) {
        Dependente d = new Dependente();
        d.setId(7L);
        d.setNome("Enzo");
        d.setEscola(escola);
        d.setResponsaveis(new ArrayList<>(List.of(responsavel)));
        return d;
    }

    private Viagem viagem(Escola escola) {
        Motorista motorista = new Motorista();
        motorista.setId(4L);
        motorista.setUsuario(usuario(USUARIO_MOTORISTA));
        Viagem v = new Viagem();
        v.setId(3L);
        v.setMotorista(motorista);
        v.setEscola(escola);
        v.setAtivo(true);
        return v;
    }

    private Escola escola(Long id, String nome) {
        Escola e = new Escola();
        e.setId(id);
        e.setNome(nome);
        return e;
    }

    private Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }
}
