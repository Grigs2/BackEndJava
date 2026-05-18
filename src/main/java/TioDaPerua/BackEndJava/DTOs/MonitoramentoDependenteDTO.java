package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

public record MonitoramentoDependenteDTO(Long dependenteId,
                                         String dependenteNome,
                                         ViagemPresencaDTO statusAtual,
                                         List<ViagemPresencaDTO> historicoRecente) {
}
