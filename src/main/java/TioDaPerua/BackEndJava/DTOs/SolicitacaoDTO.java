package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Solicitacao;
import java.time.LocalDateTime;

public record SolicitacaoDTO(Long id,
                            Long viagemId,
                            Long dependenteId,
                            Long responsavelId,
                            boolean respondido,
                            boolean aceito,
                            LocalDateTime dataInicio,
                            LocalDateTime dataFim) {

    public static SolicitacaoDTO toDTO(Solicitacao s) {
        if (s == null) return null;
        return new SolicitacaoDTO(
                s.getId(),
                s.getViagem() != null ? s.getViagem().getId() : null,
                s.getDependente() != null ? s.getDependente().getId() : null,
                s.getResponsavel() != null ? s.getResponsavel().getId() : null,
                s.isRespondido(),
                s.isAceito(),
                s.getDataInicio(),
                s.getDataFim()
        );
    }

    public static Solicitacao toEntity(SolicitacaoDTO dto) {
        if (dto == null) return null;
        Solicitacao s = new Solicitacao();
        s.setId(dto.id());
        s.setRespondido(dto.respondido());
        s.setAceito(dto.aceito());
        s.setDataInicio(dto.dataInicio());
        s.setDataFim(dto.dataFim());
        return s;
    }
}
