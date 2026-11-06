package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;

/** Resposta do motorista a um pedido de transporte feito pelo responsável. */
public record ResponderSolicitacaoDTO(@NotNull Boolean aceito) {
}
