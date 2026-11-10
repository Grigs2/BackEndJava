package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Avaliacao;

import java.time.LocalDateTime;
import java.util.List;

public record AvaliacaoDTO(Long id, Integer nota, String comentario, String responsavelNome, LocalDateTime criadoEm) {

    public static AvaliacaoDTO toDTO(Avaliacao avaliacao) {
        return new AvaliacaoDTO(
                avaliacao.getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getResponsavel() != null ? avaliacao.getResponsavel().getNome() : null,
                avaliacao.getCriadoEm());
    }

    /** Resumo exibido ao responsável: média (1 casa decimal), total e as avaliações mais recentes. */
    public record Resumo(Double media, long total, List<AvaliacaoDTO> avaliacoes) {
    }
}
