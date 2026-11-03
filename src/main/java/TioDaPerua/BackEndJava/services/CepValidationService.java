package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;

public interface CepValidationService {
    StatusValidacaoEndereco validarExistencia(String cep);
}
