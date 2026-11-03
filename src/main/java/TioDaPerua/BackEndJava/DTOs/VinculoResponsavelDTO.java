package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ConviteResponsavel;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;

import java.time.Instant;

public record VinculoResponsavelDTO(
        Long responsavelId,
        String nome,
        String email,
        String categoria,
        boolean responsavelCriador,
        Instant vinculadoEm) {

    public static VinculoResponsavelDTO toDTO(
            Dependente dependente,
            Responsavel responsavel,
            ConviteResponsavel convite) {
        return new VinculoResponsavelDTO(
                responsavel.getId(),
                responsavel.getNome(),
                responsavel.getUsuario() != null ? responsavel.getUsuario().getEmail() : null,
                convite != null && convite.getCategoria() != null ? convite.getCategoria().name() : null,
                dependente.getResponsavelCriador() != null
                        && dependente.getResponsavelCriador().getId().equals(responsavel.getId()),
                convite != null ? convite.getRespondidoEm() : null);
    }
}
