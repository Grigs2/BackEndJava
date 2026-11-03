package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class EnderecoOperacionalService {

    private final CepValidationService cepValidationService;

    public EnderecoOperacionalService(CepValidationService cepValidationService) {
        this.cepValidationService = cepValidationService;
    }

    public EnderecoOperacional validar(EnderecoOperacional endereco) {
        if (endereco == null) {
            throw new BusinessException("Endereço operacional estruturado é obrigatório");
        }

        String cep = endereco.getCep() == null ? "" : endereco.getCep().replaceAll("\\D", "");
        if (!cep.matches("\\d{8}")) {
            throw new BusinessException("CEP deve conter 8 dígitos");
        }
        endereco.setCep(cep);
        endereco.setLogradouro(exigirTexto(endereco.getLogradouro(), "Logradouro"));
        endereco.setNumero(exigirTexto(endereco.getNumero(), "Número"));
        endereco.setBairro(exigirTexto(endereco.getBairro(), "Bairro"));
        endereco.setCidade(exigirTexto(endereco.getCidade(), "Cidade"));
        String uf = exigirTexto(endereco.getUf(), "UF").toUpperCase();
        if (!uf.matches("[A-Z]{2}")) {
            throw new BusinessException("UF deve conter 2 letras");
        }
        endereco.setUf(uf);
        if (endereco.getComplemento() != null) {
            endereco.setComplemento(endereco.getComplemento().trim());
        }
        StatusValidacaoEndereco status = cepValidationService.validarExistencia(cep);
        endereco.setStatusValidacao(status);
        return endereco;
    }

    public void atualizarUsuario(Usuario usuario, EnderecoOperacional enderecoValidado) {
        EnderecoOperacional atual = usuario.getEnderecoOperacional();
        if (atual == null) {
            usuario.setEnderecoOperacional(enderecoValidado);
            return;
        }
        atual.setCep(enderecoValidado.getCep());
        atual.setLogradouro(enderecoValidado.getLogradouro());
        atual.setNumero(enderecoValidado.getNumero());
        atual.setComplemento(enderecoValidado.getComplemento());
        atual.setBairro(enderecoValidado.getBairro());
        atual.setCidade(enderecoValidado.getCidade());
        atual.setUf(enderecoValidado.getUf());
        atual.setStatusValidacao(enderecoValidado.getStatusValidacao());
    }

    private String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(campo + " é obrigatório");
        }
        return valor.trim();
    }
}
