package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import TioDaPerua.BackEndJava.entities.Solicitacao;
import jakarta.validation.constraints.NotBlank;

public record SolicitacaoDTO(Long id,
                            @NotBlank(message = "ID da viagem está nullo") Long viagemId,
                            @NotBlank(message = "ID do dependente está nullo") Long dependenteId,
                            @NotBlank(message = "ID do responsável está nullo") Long responsavelId,
                            boolean respondido,
                            boolean aceito,
                            @JsonFormat(pattern = "yyyy-MM-dd")LocalDateTime dataInicio,
                            @JsonFormat(pattern = "yyyy-MM-dd")LocalDateTime dataFim) {

    public static SolicitacaoDTO toDTO(Solicitacao s) {
        if (s == null) return null;
        return new SolicitacaoDTO(
                s.getId(),
                s.getViagem().getId(),
                s.getDependente().getId(),
                s.getResponsavel().getId(),
                s.isRespondido(),
                s.isAceito(),
                s.getDataInicio(),
                s.getDataFim()
        );
    }

    // public static Solicitacao toEntity(SolicitacaoDTO dto) {
    //     if (dto == null) return null;
    //     Solicitacao s = new Solicitacao();
    //     s.setId(dto.id());
    //     s.setRespondido(dto.respondido());
    //     s.setAceito(dto.aceito());
    //     s.setDataInicio(dto.dataInicio());
    //     s.setDataFim(dto.dataFim());
    //     return s;
    // }
}
