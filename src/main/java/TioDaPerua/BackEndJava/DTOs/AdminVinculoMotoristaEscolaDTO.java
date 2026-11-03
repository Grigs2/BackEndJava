package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;

import java.time.Instant;

public record AdminVinculoMotoristaEscolaDTO(
        Long id,
        Long escolaId,
        Long motoristaId,
        StatusVinculoEscola status,
        Instant criadoEm,
        Instant atualizadoEm) {
}
