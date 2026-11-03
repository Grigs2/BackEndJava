package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastroFase2ValidationTest {

    @Mock private MotoristaRepository motoristaRepository;
    @Mock private EscolaRepository escolaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TermosService termosService;
    @Mock private CadastroUsuarioValidator cadastroUsuarioValidator;
    @Mock private EnderecoOperacionalService enderecoOperacionalService;
    @InjectMocks private MotoristaService motoristaService;
    @InjectMocks private EscolaService escolaService;

    @Test
    void requiresMotoristsToBeAtLeastEighteen() {
        Motorista motorista = motorista(LocalDate.now().minusYears(18).plusDays(1));

        assertThrows(BusinessException.class, () -> motoristaService.cadastraMotorista(motorista));
    }

    @Test
    void requiresStructuredOperationalAddressForNewMotoristRegistration() {
        Motorista motorista = motorista(LocalDate.now().minusYears(18));
        motorista.getUsuario().setEnderecoOperacional(null);
        when(motoristaRepository.existsByCpf("52998224725")).thenReturn(false);

        assertThrows(BusinessException.class, () -> motoristaService.cadastraMotorista(motorista));
    }

    @Test
    void acceptsMotoristAtExactEighteenYearBoundary() {
        Motorista motorista = motorista(LocalDate.now().minusYears(18));
        when(enderecoOperacionalService.validar(any(EnderecoOperacional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(motoristaRepository.existsByCpf("52998224725")).thenReturn(false);
        when(passwordEncoder.encode("Senha segura 123!")).thenReturn("bcrypt-hash");
        when(motoristaRepository.save(any(Motorista.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Motorista salvo = motoristaService.cadastraMotorista(motorista);

        assertEquals("52998224725", salvo.getCpf());
        assertEquals("bcrypt-hash", salvo.getUsuario().getSenha());
        verify(termosService).registrarAceite(salvo.getUsuario(), true);
    }

    @Test
    void requiresUniqueValidSchoolCnpj() {
        Escola escola = new Escola();
        escola.setNome("Escola");
        escola.setCnpj("11.222.333/0001-81");
        escola.setUsuario(new Usuario("escola@example.com", "secret", "Rua", "11999999999", "ESCOLA"));
        escola.getUsuario().setEnderecoOperacional(enderecoOperacional());
        escola.getUsuario().setAceitouTermos(true);
        when(escolaRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(BusinessException.class, () -> escolaService.cadastrarEscola(escola));
    }

    @Test
    void enforcesPasswordPolicyForNewMotoristAccounts() {
        Motorista motorista = motorista(LocalDate.now().minusYears(18));
        motorista.getUsuario().setSenha("curta");
        when(motoristaRepository.existsByCpf("52998224725")).thenReturn(false);

        assertThrows(BusinessException.class, () -> motoristaService.cadastraMotorista(motorista));
    }

    private Motorista motorista(LocalDate dataNascimento) {
        Motorista motorista = new Motorista();
        motorista.setNome("Motorista");
        motorista.setDataNascimento(dataNascimento);
        motorista.setCpf("529.982.247-25");
        Usuario usuario = new Usuario("motorista@example.com", "Senha segura 123!", "Rua", "11999999999", "MOTORISTA");
        usuario.setEnderecoOperacional(enderecoOperacional());
        usuario.setAceitouTermos(true);
        motorista.setUsuario(usuario);
        return motorista;
    }

    private EnderecoOperacional enderecoOperacional() {
        EnderecoOperacional endereco = new EnderecoOperacional();
        endereco.setCep("12345678");
        endereco.setLogradouro("Rua A");
        endereco.setNumero("12");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setUf("SP");
        return endereco;
    }
}
