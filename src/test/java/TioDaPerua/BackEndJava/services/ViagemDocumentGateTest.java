package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemDocumentGateTest {

    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private EnderecoDependenteRepository enderecoDependenteRepository;
    @Mock private ViagemEscolaRepository viagemEscolaRepository;
    @Mock private HistoricoViagemDiaRepository historicoViagemDiaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private DocumentoMotoristaService documentoMotoristaService;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @InjectMocks private ViagemService service;

    @Test
    void validatesDocumentsWhenExecutionMovesToInProgress() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        prepararProntidao(execucao);
        when(viagemDiaRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(execucao));
        when(viagemDiaRepository.save(any(ViagemDia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentoMotoristaService.listarPendenciasParaInicio(42L)).thenReturn(List.of());
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(execucao.getViagem().getMotorista().getUsuario()));

        service.alterarStatusViagemDia(9L, "EM_ANDAMENTO", null, 100L);

        verify(documentoMotoristaService).listarPendenciasParaInicio(42L);
    }

    @Test
    void doesNotAllowNoOpTransitionToPlanned() {
        ViagemDia execucao = execucao(StatusViagemDia.PLANEJADA);
        when(viagemDiaRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(execucao));

        org.junit.jupiter.api.Assertions.assertThrows(
                TioDaPerua.BackEndJava.exceptions.ConflictException.class,
                () -> service.alterarStatusViagemDia(9L, "PLANEJADA", null, 100L));

        verifyNoInteractions(documentoMotoristaService);
    }

    private ViagemDia execucao(StatusViagemDia status) {
        Motorista motorista = new Motorista();
        motorista.setId(42L);
        Usuario usuarioMotorista = new Usuario();
        usuarioMotorista.setId(100L);
        EnderecoOperacional enderecoMotorista = new EnderecoOperacional();
        enderecoMotorista.setStatusValidacao(StatusValidacaoEndereco.VALIDADO);
        usuarioMotorista.setEnderecoOperacional(enderecoMotorista);
        motorista.setUsuario(usuarioMotorista);
        Veiculo veiculo = new Veiculo();
        veiculo.setCapacidade(2);
        motorista.setVeiculo(veiculo);
        Viagem viagem = new Viagem();
        viagem.setId(77L);
        viagem.setPeriodo(Periodo.MANHA_IDA);
        viagem.setAtivo(true);
        viagem.setMotorista(motorista);
        ViagemDia execucao = new ViagemDia();
        execucao.setId(9L);
        execucao.setViagem(viagem);
        execucao.setStatus(status);
        return execucao;
    }

    private void prepararProntidao(ViagemDia execucao) {
        Escola escola = new Escola();
        escola.setId(12L);
        Usuario usuarioEscola = new Usuario();
        EnderecoOperacional enderecoEscola = new EnderecoOperacional();
        enderecoEscola.setStatusValidacao(StatusValidacaoEndereco.VALIDADO);
        usuarioEscola.setEnderecoOperacional(enderecoEscola);
        escola.setUsuario(usuarioEscola);
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        dependente.setEscola(escola);
        when(viagemDependenteRepository.buscarDependentesCompletos(77L))
                .thenReturn(List.of(new ViagemDependente(1L, execucao.getViagem(), dependente, true)));
        EnderecoDependente enderecoDependente = new EnderecoDependente();
        enderecoDependente.setDependente(dependente);
        enderecoDependente.setFinalidade(FinalidadeEndereco.EMBARQUE);
        enderecoDependente.setStatusValidacao(StatusValidacaoEndereco.VALIDADO);
        when(enderecoDependenteRepository.findByDependenteIdInAndAtivoTrue(List.of(5L)))
                .thenReturn(List.of(enderecoDependente));
        when(viagemEscolaRepository.listarIdsEscolasAtivasDoMotorista(
                42L, Set.of(12L), TioDaPerua.BackEndJava.enums.StatusVinculoEscola.ATIVO))
                .thenReturn(List.of(12L));
    }
}
