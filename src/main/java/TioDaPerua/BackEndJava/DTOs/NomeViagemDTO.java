package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.Size;

/** Nome editável da viagem. Vazio ou nulo volta ao nome padrão de ida/volta. */
public record NomeViagemDTO(@Size(max = 200) String nome) {
}
