package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnderecoOperacionalServiceTest {

    @Mock
    private CepValidationService cepValidationService;

    @InjectMocks
    private EnderecoOperacionalService service;

    @Test
    void normalizesAddressAndKeepsPendingWhenCepProviderIsUnavailable() {
        EnderecoOperacional endereco = endereco("12345-678");
        when(cepValidationService.validarExistencia("12345678"))
                .thenReturn(StatusValidacaoEndereco.PENDENTE);

        EnderecoOperacional validado = service.validar(endereco);

        assertEquals("12345678", validado.getCep());
        assertEquals("Rua A", validado.getLogradouro());
        assertEquals("12", validado.getNumero());
        assertEquals("SP", validado.getUf());
        assertEquals(StatusValidacaoEndereco.PENDENTE, validado.getStatusValidacao());
        verify(cepValidationService).validarExistencia("12345678");
    }

    @Test
    void rejectsMalformedCepBeforeCallingProvider() {
        EnderecoOperacional endereco = endereco("1234");

        assertThrows(BusinessException.class, () -> service.validar(endereco));
    }

    private EnderecoOperacional endereco(String cep) {
        EnderecoOperacional endereco = new EnderecoOperacional();
        endereco.setCep(cep);
        endereco.setLogradouro(" Rua A ");
        endereco.setNumero(" 12 ");
        endereco.setBairro(" Centro ");
        endereco.setCidade(" São Paulo ");
        endereco.setUf("sp");
        return endereco;
    }
}
