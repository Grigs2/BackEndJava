package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

public record ProntidaoViagemDTO(
        Long idViagem,
        boolean pronta,
        List<String> bloqueios) {
}
