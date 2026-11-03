package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ConviteResponsavel;

import java.time.Instant;

public record ConviteResponsavelDTO(
        Long id,
        Long dependenteId,
        String dependenteNome,
        Long convidadoId,
        String convidadoNome,
        String convidadoEmail,
        Long convidadorId,
        String convidadorNome,
        String categoria,
        String status,
        Instant criadoEm,
        Instant expiraEm,
        Instant respondidoEm,
        Instant revogadoEm) {

    public static ConviteResponsavelDTO toDTO(ConviteResponsavel convite) {
        return new ConviteResponsavelDTO(
                convite.getId(),
                convite.getDependente().getId(),
                convite.getDependente().getNome(),
                convite.getConvidado().getId(),
                convite.getConvidado().getNome(),
                convite.getConvidado().getUsuario().getEmail(),
                convite.getConvidador() == null ? null : convite.getConvidador().getId(),
                convite.getConvidador() == null ? null : convite.getConvidador().getNome(),
                convite.getCategoria() == null ? null : convite.getCategoria().name(),
                convite.getStatus().name(),
                convite.getCriadoEm(),
                convite.getExpiraEm(),
                convite.getRespondidoEm(),
                convite.getRevogadoEm());
    }
}
