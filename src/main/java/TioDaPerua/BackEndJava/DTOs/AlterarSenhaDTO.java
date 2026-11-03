package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.validation.SenhaValida;
import jakarta.validation.constraints.NotBlank;

public record AlterarSenhaDTO(
        @NotBlank String senhaAtual,
        @NotBlank @SenhaValida String novaSenha
) {
}
