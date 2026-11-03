package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.entities.ViagemDependente;
import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoOrdemParadaRepository;
import TioDaPerua.BackEndJava.repositories.HistoricoViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDiaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemDependenteRepository;
import TioDaPerua.BackEndJava.repositories.ViagemEscolaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemParadaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemPresencaRepository;
import TioDaPerua.BackEndJava.repositories.ViagemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViagemEnderecoSnapshotTest {

    @Mock private ViagemRepository viagemRepository;
    @Mock private ViagemDependenteRepository viagemDependenteRepository;
    @Mock private ViagemDiaRepository viagemDiaRepository;
    @Mock private ViagemPresencaRepository viagemPresencaRepository;
    @Mock private EnderecoDependenteRepository enderecoDependenteRepository;
    @Mock private SolicitacaoRepository solicitacaoRepository;
    @Mock private ViagemEscolaRepository viagemEscolaRepository;
    @Mock private ViagemParadaRepository viagemParadaRepository;
    @Mock private HistoricoViagemDiaRepository historicoViagemDiaRepository;
    @Mock private HistoricoOrdemParadaRepository historicoOrdemParadaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ResponsavelRepository responsavelRepository;
    @Mock private NotificacaoService notificacaoService;
    @InjectMocks private ViagemService service;

    @Test
    void copiesAddressIntoPresenceWhenDailyExecutionIsCreated() {
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        Responsavel responsavel = new Responsavel();
        Usuario usuario = new Usuario();
        usuario.setEndereco("Endereço antigo");
        responsavel.setUsuario(usuario);
        dependente.setResponsaveis(List.of(responsavel));

        Viagem viagem = new Viagem();
        viagem.setId(77L);
        viagem.setPeriodo(Periodo.MANHA_IDA);
        viagem.setAtivo(true);
        Motorista motorista = new Motorista();
        Usuario usuarioMotorista = new Usuario();
        usuarioMotorista.setId(100L);
        usuarioMotorista.setEnderecoOperacional(new EnderecoOperacional());
        motorista.setUsuario(usuarioMotorista);
        viagem.setMotorista(motorista);
        when(viagemRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(viagem));
        when(viagemDiaRepository.findByViagemIdAndData(77L, LocalDate.now())).thenReturn(Optional.empty());
        when(viagemDiaRepository.save(any(ViagemDia.class)))
                .thenAnswer(invocation -> {
                    ViagemDia salvo = invocation.getArgument(0);
                    salvo.setId(90L);
                    return salvo;
                });
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(usuarioMotorista));
        when(viagemDependenteRepository.buscarDependentesCompletos(77L))
                .thenReturn(List.of(new ViagemDependente(1L, viagem, dependente, true)));

        EnderecoDependente endereco = new EnderecoDependente();
        endereco.setDependente(dependente);
        endereco.setCep("12345678");
        endereco.setLogradouro("Rua Nova");
        endereco.setNumero("12");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setUf("SP");
        endereco.setFinalidade(FinalidadeEndereco.EMBARQUE);
        when(enderecoDependenteRepository.findByDependenteIdInAndAtivoTrue(List.of(5L)))
                .thenReturn(List.of(endereco));
        when(viagemPresencaRepository.save(any(ViagemPresenca.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.iniciarViagemDia(77L, 100L);

        ArgumentCaptor<ViagemPresenca> captor = ArgumentCaptor.forClass(ViagemPresenca.class);
        verify(viagemPresencaRepository).save(captor.capture());
        assertEquals("Rua Nova, 12 - Centro, São Paulo/SP - CEP 12345678",
                captor.getValue().getEnderecoEmbarqueSnapshot().formatar());
        assertEquals("Endereço antigo", captor.getValue().getEnderecoDesembarqueLegadoSnapshot());
    }

    @Test
    void routeUsesTheStoredSnapshotInsteadOfCurrentGuardianAddress() {
        Dependente dependente = new Dependente();
        dependente.setId(5L);
        dependente.setNome("Criança");
        Responsavel responsavel = new Responsavel();
        responsavel.setNome("Responsável");
        Usuario usuario = new Usuario();
        usuario.setEndereco("Endereço atual");
        responsavel.setUsuario(usuario);
        dependente.setResponsaveis(List.of(responsavel));

        Viagem viagem = new Viagem();
        viagem.setId(77L);
        viagem.setPeriodo(Periodo.MANHA_IDA);
        ViagemDia viagemDia = new ViagemDia();
        viagemDia.setId(90L);
        viagemDia.setViagem(viagem);
        when(viagemDiaRepository.findById(90L)).thenReturn(Optional.of(viagemDia));
        when(viagemParadaRepository.listarPorViagemDia(90L)).thenReturn(List.of());
        when(viagemDependenteRepository.buscarDependentesCompletos(77L))
                .thenReturn(List.of(new ViagemDependente(1L, viagem, dependente, true)));

        ViagemPresenca presenca = new ViagemPresenca();
        presenca.setDependente(dependente);
        presenca.setEnderecoEmbarqueSnapshot(snapshot("Endereço histórico"));
        when(viagemPresencaRepository.findByViagemDiaId(90L)).thenReturn(List.of(presenca));

        var paradas = service.gerarParadasViagemDia(90L);

        assertEquals("Endereço histórico, 12 - Centro, São Paulo/SP - CEP 12345678",
                paradas.get(1).endereco());
    }

    private TioDaPerua.BackEndJava.entities.EnderecoSnapshot snapshot(String logradouro) {
        var snapshot = new TioDaPerua.BackEndJava.entities.EnderecoSnapshot();
        snapshot.setCep("12345678");
        snapshot.setLogradouro(logradouro);
        snapshot.setNumero("12");
        snapshot.setBairro("Centro");
        snapshot.setCidade("São Paulo");
        snapshot.setUf("SP");
        return snapshot;
    }
}
