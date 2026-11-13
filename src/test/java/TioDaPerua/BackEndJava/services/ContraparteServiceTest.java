package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.ContraparteDTO;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;
import TioDaPerua.BackEndJava.repositories.AvaliacaoRepository;
import TioDaPerua.BackEndJava.repositories.EnderecoDependenteRepository;
import TioDaPerua.BackEndJava.repositories.SolicitacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** Pop-up de dados da outra parte: só participantes veem; nome e telefone completos, o resto mascarado. */
@ExtendWith(MockitoExtension.class)
class ContraparteServiceTest {

    private static final long USUARIO_MOTORISTA = 20L;
    private static final long USUARIO_RESPONSAVEL = 10L;

    @Mock private SolicitacaoRepository solicitacaoRepository;
    @Mock private AvaliacaoRepository avaliacaoRepository;
    @Mock private EnderecoDependenteRepository enderecoDependenteRepository;
    @InjectMocks private ContraparteService service;

    @Test
    void responsavelVeMotoristaComDadosMascarados() {
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(solicitacao()));
        when(avaliacaoRepository.listarDoMotorista(4L)).thenReturn(List.of());

        ContraparteDTO c = service.motoristaDaSolicitacao(5L, USUARIO_RESPONSAVEL);

        assertEquals("MOTORISTA", c.perfil());
        assertEquals("Carlos Souza", c.nome());
        assertEquals("(11) 97777-6666", c.telefone());
        assertEquals("ca***@exemplo.dev", c.emailMascarado());
        assertEquals("***.444.777-**", c.cpfMascarado());
        assertEquals("ABC****", c.veiculo().placaMascarada());
        assertEquals("Ipiranga", c.bairro());
        assertEquals(List.of("EMEF Pedro II"), c.escolasAtendidas());
        assertNull(c.avaliacaoMedia());
    }

    @Test
    void motoristaVeResponsavelEAluno() {
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(solicitacao()));
        when(enderecoDependenteRepository.findByDependenteIdAndAtivoTrueOrderById(7L)).thenReturn(List.of());

        ContraparteDTO c = service.responsavelDaSolicitacao(5L, USUARIO_MOTORISTA);

        assertEquals("RESPONSAVEL", c.perfil());
        assertEquals("Ana Souza", c.nome());
        assertEquals("(11) 98888-7777", c.telefone());
        assertEquals("***.982.247-**", c.cpfMascarado());
        assertEquals("Enzo", c.aluno().nome());
        assertEquals("EMEF Pedro II", c.aluno().escola());
        assertNull(c.veiculo());
    }

    @Test
    void quemNaoParticipaNaoVe() {
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(solicitacao()));
        assertThrows(AccessDeniedException.class, () -> service.motoristaDaSolicitacao(5L, 999L));
        assertThrows(AccessDeniedException.class, () -> service.responsavelDaSolicitacao(5L, 999L));
    }

    @Test
    void mascaras() {
        assertEquals("***.982.247-**", ContraparteService.mascararCpf("52998224725"));
        assertNull(ContraparteService.mascararCpf("123"));
        assertEquals("a***@x.com", ContraparteService.mascararEmail("a@x.com"));
        assertEquals("XYZ****", ContraparteService.mascararPlaca("xyz-5678"));
    }

    private Solicitacao solicitacao() {
        Escola escola = new Escola();
        escola.setNome("EMEF Pedro II");

        Usuario uMot = new Usuario();
        uMot.setId(USUARIO_MOTORISTA);
        uMot.setEmail("carlos@exemplo.dev");
        uMot.setTelefone("(11) 97777-6666");
        EnderecoOperacional end = new EnderecoOperacional();
        end.setBairro("Ipiranga");
        end.setCidade("São Paulo");
        end.setUf("SP");
        uMot.setEnderecoOperacional(end);
        Veiculo veiculo = new Veiculo();
        veiculo.setModelo("Sprinter");
        veiculo.setPlaca("ABC1D23");
        veiculo.setCapacidade(15);
        Motorista m = new Motorista();
        m.setId(4L);
        m.setNome("Carlos Souza");
        m.setCpf("111.444.777-35");
        m.setUsuario(uMot);
        m.setVeiculo(veiculo);

        Viagem v = new Viagem();
        v.setId(3L);
        v.setMotorista(m);
        v.setPeriodo(Periodo.MANHA_IDA);
        v.setEscolas(List.of(escola));

        Usuario uResp = new Usuario();
        uResp.setId(USUARIO_RESPONSAVEL);
        uResp.setEmail("ana@exemplo.dev");
        uResp.setTelefone("(11) 98888-7777");
        Responsavel r = new Responsavel();
        r.setId(1L);
        r.setNome("Ana Souza");
        r.setCpf("529.982.247-25");
        r.setUsuario(uResp);

        Dependente d = new Dependente();
        d.setId(7L);
        d.setNome("Enzo");
        d.setDataNascimento(LocalDate.now().minusYears(9));
        d.setEscola(escola);
        d.setResponsaveis(new ArrayList<>(List.of(r)));

        Solicitacao s = new Solicitacao();
        s.setId(5L);
        s.setViagem(v);
        s.setDependente(d);
        s.setResponsavel(r);
        return s;
    }
}
