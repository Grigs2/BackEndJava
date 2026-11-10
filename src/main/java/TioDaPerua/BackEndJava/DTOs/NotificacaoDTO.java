package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Notificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * Notificação. Os campos tipo, prioridade, viagemDiaId, dependenteId e lidaEm são preenchidos pelo servidor
 * (avisos automáticos) e ignorados quando o DTO chega como corpo de um aviso manual.
 */
public record NotificacaoDTO(Long id,
                            @NotBlank String titulo,
                            @NotBlank String mensagem,
                            LocalDateTime dataCriacao,
                            boolean visto,
                            UsuarioResumoDTO remetente,
                            @NotNull @Positive Long remetenteId,
                            @NotNull @Positive Long destinatarioId,
                            String tipo,
                            String prioridade,
                            Long viagemDiaId,
                            Long dependenteId,
                            LocalDateTime lidaEm) {

    public static NotificacaoDTO toDTO(Notificacao n, UsuarioResumoDTO remetenteResumo) {
        if (n == null) return null;
        return new NotificacaoDTO(
                n.getId(),
                n.getTitulo(),
                n.getMensagem(),
                n.getData(),
                n.getVisto() != null ? n.getVisto() : false,
                remetenteResumo,
                n.getRemetente() != null ? n.getRemetente().getId() : null,
                n.getDestinatario() != null ? n.getDestinatario().getId() : null,
                n.getTipo() != null ? n.getTipo() : "GERAL",
                n.getPrioridade() != null ? n.getPrioridade() : "NORMAL",
                n.getViagemDiaId(),
                n.getDependenteId(),
                n.getLidaEm()
        );
    }

    public static Notificacao toEntity(NotificacaoDTO dto) {
        if (dto == null) return null;
        Notificacao n = new Notificacao();
        n.setId(dto.id());
        n.setTitulo(dto.titulo());
        n.setMensagem(dto.mensagem());
        n.setData(dto.dataCriacao());
        n.setVisto(dto.visto());
        return n;
    }
}
