package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteDTO;
import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteRequestDTO;
import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnderecoDependenteServiceTest {

    @Mock private EnderecoDependenteTransaction transaction;
    @Mock private CepValidationService cepValidationService;

    @Test
    void validatesCepBeforeOpeningTheAddressTransaction() {
        EnderecoDependenteService service = new EnderecoDependenteService(transaction, cepValidationService);
        EnderecoDependenteRequestDTO request = request("12345-678");
        when(cepValidationService.validarExistencia("12345678"))
                .thenReturn(StatusValidacaoEndereco.PENDENTE);

        service.adicionar(5L, 10L, request);

        verify(cepValidationService).validarExistencia("12345678");
        verify(transaction).adicionar(5L, 10L, request, StatusValidacaoEndereco.PENDENTE);
    }

    @Test
    void rejectsMalformedCepBeforeCallingExternalProvider() {
        EnderecoDependenteService service = new EnderecoDependenteService(transaction, cepValidationService);

        assertThrows(BusinessException.class, () -> service.adicionar(5L, 10L, request("12")));

        verifyNoInteractions(cepValidationService, transaction);
    }

    private EnderecoDependenteRequestDTO request(String cep) {
        return new EnderecoDependenteRequestDTO(
                cep, "Rua", "1", null, "Centro", "Cidade", "SP", FinalidadeEndereco.EMBARQUE);
    }
}
