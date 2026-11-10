package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Solicitacao;
import java.time.LocalDateTime;

/**
 * Solicitação de transporte. "origem" diz quem pediu (MOTORISTA ou RESPONSAVEL); quem não pediu é quem responde.
 * Os campos de nome existem para exibição e são ignorados quando o DTO chega como corpo de requisição.
 */
public record SolicitacaoDTO(Long id,
                            Long viagemId,
                            Long dependenteId,
                            Long responsavelId,
                            boolean respondido,
                            boolean aceito,
                            LocalDateTime dataInicio,
                            LocalDateTime dataFim,
                            String origem,
                            String dependenteNome,
                            String viagemNome,
                            String viagemPeriodo,
                            Long motoristaId,
                            String motoristaNome,
                            String responsavelNome,
                            String status,
                            LocalDateTime dataExpiracao,
                            Boolean compartilhamentoAutorizado) {

    public static SolicitacaoDTO toDTO(Solicitacao s) {
        if (s == null) return null;
        var viagem = s.getViagem();
        var motorista = viagem != null ? viagem.getMotorista() : null;
        return new SolicitacaoDTO(
                s.getId(),
                viagem != null ? viagem.getId() : null,
                s.getDependente() != null ? s.getDependente().getId() : null,
                s.getResponsavel() != null ? s.getResponsavel().getId() : null,
                s.isRespondido(),
                s.isAceito(),
                s.getDataInicio(),
                s.getDataFim(),
                s.getOrigemEfetiva().name(),
                s.getDependente() != null ? s.getDependente().getNome() : null,
                viagem != null ? viagem.getNome() : null,
                viagem != null && viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null,
                motorista != null ? motorista.getId() : null,
                motorista != null ? motorista.getNome() : null,
                s.getResponsavel() != null ? s.getResponsavel().getNome() : null,
                s.getStatusEfetivo().name(),
                s.getDataExpiracao(),
                s.getCompartilhamentoAutorizado()
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
