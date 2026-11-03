package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRedefinicaoSenhaDTO(@NotBlank @Email String email) {
}
