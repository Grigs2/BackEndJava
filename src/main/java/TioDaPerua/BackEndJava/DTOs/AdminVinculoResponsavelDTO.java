package TioDaPerua.BackEndJava.DTOs;

import java.time.Instant;

public record AdminVinculoResponsavelDTO(
        Long dependenteId,
        Long responsavelId,
        String status,
        String categoria,
        boolean responsavelCriador,
        Instant vinculadoEm) {
}
