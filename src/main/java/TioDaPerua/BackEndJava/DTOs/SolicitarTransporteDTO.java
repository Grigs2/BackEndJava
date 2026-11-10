package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;

/** Pedido de vaga do responsável; a confirmação de compartilhamento é obrigatória. */
public record SolicitarTransporteDTO(@NotNull Boolean compartilhamentoAutorizado) {
}
