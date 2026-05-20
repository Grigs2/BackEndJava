package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

public record ViagemDetalhadaDTO(Long id,
                                String motoristaNome,
                                String periodo,
                                List<DependenteResumoDTO> dependentes) {
}
