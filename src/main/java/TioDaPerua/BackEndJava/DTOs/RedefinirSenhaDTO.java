package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.validation.SenhaValida;
import jakarta.validation.constraints.NotBlank;

public record RedefinirSenhaDTO(
        @NotBlank String token,
        @NotBlank @SenhaValida String novaSenha
) {
}
