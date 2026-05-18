package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.TipoParada;
import java.util.List;

public record ParadaDTO(int ordem,
                       TipoParada tipoParada,
                       String nomeLocal,
                       String endereco,
                       List<DependenteParadaDTO> listaDependentes) {
}
