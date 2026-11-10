package TioDaPerua.BackEndJava.DTOs;

import jakarta.validation.constraints.NotNull;

/**
 * Resposta a uma solicitação de transporte.
 * compartilhamentoAutorizado é obrigatório para o responsável aceitar um convite do motorista.
 */
public record ResponderSolicitacaoDTO(@NotNull Boolean aceito, Boolean compartilhamentoAutorizado) {
}
