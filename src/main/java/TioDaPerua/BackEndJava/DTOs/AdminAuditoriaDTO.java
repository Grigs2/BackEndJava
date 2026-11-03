package TioDaPerua.BackEndJava.DTOs;

import java.time.Instant;

public record AdminAuditoriaDTO(
        Long id,
        Long atorUsuarioId,
        String acao,
        String tipoAlvo,
        Long alvoId,
        String resultado,
        String motivo,
        Instant ocorridoEm) {
}
