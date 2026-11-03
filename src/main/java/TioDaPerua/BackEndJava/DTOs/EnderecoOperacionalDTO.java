package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoOperacionalDTO(
        Long id,
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve conter 8 dígitos") String cep,
        @NotBlank @Size(max = 150) String logradouro,
        @NotBlank @Size(max = 30) String numero,
        @Size(max = 100) String complemento,
        @NotBlank @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve conter 2 letras") String uf,
        StatusValidacaoEndereco statusValidacao) {

    public static EnderecoOperacionalDTO toDTO(EnderecoOperacional endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoOperacionalDTO(
                endereco.getId(),
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                endereco.getStatusValidacao());
    }

    public static EnderecoOperacional toEntity(EnderecoOperacionalDTO dto) {
        if (dto == null) {
            return null;
        }
        EnderecoOperacional endereco = new EnderecoOperacional();
        endereco.setCep(dto.cep());
        endereco.setLogradouro(dto.logradouro());
        endereco.setNumero(dto.numero());
        endereco.setComplemento(dto.complemento());
        endereco.setBairro(dto.bairro());
        endereco.setCidade(dto.cidade());
        endereco.setUf(dto.uf());
        return endereco;
    }
}
