package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotBlank;

public record EscolaResumoDTO(Long id, @NotBlank(message = "Nome da escola está nullo") String nome) {
}
