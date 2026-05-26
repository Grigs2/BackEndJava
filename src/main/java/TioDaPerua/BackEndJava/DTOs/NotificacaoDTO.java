package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Notificacao;
import java.time.LocalDateTime;

public record NotificacaoDTO(Long id,
                            String titulo,
                            String mensagem,
                            LocalDateTime dataCriacao,
                            boolean visto,
                            UsuarioResumoDTO remetente,
                            Long remetenteId,
                            Long destinatarioId) {

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
                n.getDestinatario() != null ? n.getDestinatario().getId() : null
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
