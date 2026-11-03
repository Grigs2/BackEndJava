package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoDependenteRequestDTO(
        @NotBlank
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve conter 8 dígitos")
        String cep,
        @NotBlank @Size(max = 150) String logradouro,
        @NotBlank @Size(max = 30) String numero,
        @Size(max = 100) String complemento,
        @NotBlank @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "UF deve conter 2 letras") String uf,
        @NotNull FinalidadeEndereco finalidade) {
}
