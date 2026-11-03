package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnderecoOperacionalPerfilTest {

    @Mock private MotoristaRepository motoristaRepository;
    @Mock private EscolaRepository escolaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CadastroUsuarioValidator cadastroUsuarioValidator;
    @Mock private EnderecoOperacionalService enderecoOperacionalService;
    @Mock private CepValidationService cepValidationService;
    @InjectMocks private MotoristaService motoristaService;
    @InjectMocks private EscolaService escolaService;

    @Test
    void omittingAddressDuringLegacyMotoristUpdatePreservesExistingAddress() {
        Motorista existente = motorista();
        EnderecoOperacional enderecoOriginal = endereco("Rua antiga", StatusValidacaoEndereco.VALIDADO);
        enderecoOriginal.setId(11L);
        existente.getUsuario().setEnderecoOperacional(enderecoOriginal);
        when(motoristaRepository.findByIdComEnderecoOperacional(1L)).thenReturn(Optional.of(existente));
        when(motoristaRepository.save(any(Motorista.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Motorista requisicao = new Motorista();
        requisicao.setId(1L);
        requisicao.setNome("Motorista atualizado");
        requisicao.setUsuario(new Usuario("novo@example.com", null, "Texto legado", "11999999999", "MOTORISTA"));

        Motorista atualizado = motoristaService.alterarInfos(requisicao);

        assertSame(enderecoOriginal, atualizado.getUsuario().getEnderecoOperacional());
        verify(enderecoOperacionalService, never()).validar(any());
    }

    @Test
    void updatesExistingSchoolAddressInPlaceAndUsesServerValidationStatus() {
        Escola existente = new Escola();
        existente.setId(2L);
        Usuario usuarioExistente = new Usuario("escola@example.com", null, "Texto legado", "11999999999", "ESCOLA");
        EnderecoOperacional enderecoOriginal = endereco("Rua antiga", StatusValidacaoEndereco.PENDENTE);
        enderecoOriginal.setId(12L);
        usuarioExistente.setEnderecoOperacional(enderecoOriginal);
        existente.setUsuario(usuarioExistente);
        when(escolaRepository.findByIdComEnderecoOperacional(2L)).thenReturn(Optional.of(existente));
        when(escolaRepository.save(any(Escola.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enderecoOperacionalService.validar(any(EnderecoOperacional.class)))
                .thenAnswer(invocation -> {
                    EnderecoOperacional endereco = invocation.getArgument(0);
                    endereco.setStatusValidacao(StatusValidacaoEndereco.VALIDADO);
                    return endereco;
                });
        doAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            EnderecoOperacional endereco = invocation.getArgument(1);
            new EnderecoOperacionalService(cepValidationService).atualizarUsuario(usuario, endereco);
            return null;
        }).when(enderecoOperacionalService).atualizarUsuario(any(Usuario.class), any(EnderecoOperacional.class));

        Escola requisicao = new Escola();
        requisicao.setId(2L);
        requisicao.setNome("Escola");
        Usuario usuarioNovo = new Usuario("escola@example.com", null, "Texto legado", "11999999999", "ESCOLA");
        usuarioNovo.setEnderecoOperacional(endereco("Rua nova", StatusValidacaoEndereco.PENDENTE));
        requisicao.setUsuario(usuarioNovo);

        Escola atualizada = escolaService.alterarInfos(requisicao);

        assertEquals(12L, atualizada.getUsuario().getEnderecoOperacional().getId());
        assertEquals("Rua nova", atualizada.getUsuario().getEnderecoOperacional().getLogradouro());
        assertEquals(StatusValidacaoEndereco.VALIDADO,
                atualizada.getUsuario().getEnderecoOperacional().getStatusValidacao());
    }

    private Motorista motorista() {
        Motorista motorista = new Motorista();
        motorista.setId(1L);
        motorista.setNome("Motorista");
        motorista.setUsuario(new Usuario("motorista@example.com", null, "Texto legado", "11999999999", "MOTORISTA"));
        motorista.getUsuario().setId(3L);
        return motorista;
    }

    private EnderecoOperacional endereco(String logradouro, StatusValidacaoEndereco status) {
        EnderecoOperacional endereco = new EnderecoOperacional();
        endereco.setCep("12345678");
        endereco.setLogradouro(logradouro);
        endereco.setNumero("12");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setUf("SP");
        endereco.setStatusValidacao(status);
        return endereco;
    }
}
