package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Escolas a acrescentar ao itinerário. Só são aceitas escolas com vínculo ativo com o motorista. */
public record AdicionarEscolasViagemDTO(@NotEmpty List<Long> escolaIds) {
}
