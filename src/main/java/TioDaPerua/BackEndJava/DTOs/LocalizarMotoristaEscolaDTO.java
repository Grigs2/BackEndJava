package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record LocalizarMotoristaEscolaDTO(
        @Email @Size(max = 254) String email,
        @Size(max = 14) String cpf) {
}
