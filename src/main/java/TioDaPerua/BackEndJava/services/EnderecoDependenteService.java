package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteDTO;
import TioDaPerua.BackEndJava.DTOs.EnderecoDependenteRequestDTO;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnderecoDependenteService {
    private final EnderecoDependenteTransaction transaction;
    private final CepValidationService cepValidationService;

    public EnderecoDependenteService(EnderecoDependenteTransaction transaction,
                                     CepValidationService cepValidationService) {
        this.transaction = transaction;
        this.cepValidationService = cepValidationService;
    }

    public List<EnderecoDependenteDTO> listar(Long dependenteId, Long usuarioId) {
        return transaction.listar(dependenteId, usuarioId);
    }

    public EnderecoDependenteDTO adicionar(Long dependenteId,
                                           Long usuarioId,
                                           EnderecoDependenteRequestDTO request) {
        String cep = normalizarCep(request);
        StatusValidacaoEndereco status = cepValidationService.validarExistencia(cep);
        return transaction.adicionar(dependenteId, usuarioId, request, status);
    }

    public EnderecoDependenteDTO alterar(Long dependenteId,
                                         Long enderecoId,
                                         Long usuarioId,
                                         EnderecoDependenteRequestDTO request) {
        String cep = normalizarCep(request);
        StatusValidacaoEndereco status = cepValidationService.validarExistencia(cep);
        return transaction.alterar(dependenteId, enderecoId, usuarioId, request, status);
    }

    public void desativar(Long dependenteId, Long enderecoId, Long usuarioId) {
        transaction.desativar(dependenteId, enderecoId, usuarioId);
    }

    private String normalizarCep(EnderecoDependenteRequestDTO request) {
        if (request == null || request.cep() == null) {
            throw new IllegalArgumentException("CEP é obrigatório");
        }
        String cep = request.cep().replace("-", "");
        if (!cep.matches("\\d{8}")) {
            throw new BusinessException("CEP deve conter 8 dígitos");
        }
        return cep;
    }
}
