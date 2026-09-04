package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import TioDaPerua.BackEndJava.entities.Notificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

public record NotificacaoDTO(Long id,
                            @NotBlank(message = "Titulo está nullo")String titulo,
                            @NotBlank(message = "Mensagem está nullo")String mensagem,
                            @NotBlank(message = "Data de criação está nullo")
                                @Past(message = "A data deve ser no passado.")
                                @JsonFormat(pattern = "yyyy-MM-dd")LocalDateTime dataCriacao,
                            boolean visto,
                            @NotBlank(message = "Remetente está nullo")UsuarioResumoDTO remetente,
                            @NotBlank(message = "ID do remetente está nullo")Long remetenteId,
                            @NotBlank(message = "ID do destinatário está nullo")Long destinatarioId) {

    public static NotificacaoDTO toDTO(Notificacao n, UsuarioResumoDTO remetenteResumo) {
        if (n == null) return null;
        return new NotificacaoDTO(
                n.getId(),
                n.getTitulo(),
                n.getMensagem(),
                n.getData(),
                n.getVisto() != null ? n.getVisto() : false,
                remetenteResumo,
                n.getRemetente().getId(),
                n.getDestinatario().getId()
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
