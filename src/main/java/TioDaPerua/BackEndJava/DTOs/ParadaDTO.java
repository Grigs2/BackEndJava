package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.enums.TipoParada;
import java.util.List;

public record ParadaDTO(Long id,
                       int ordem,
                       TipoParada tipoParada,
                       String nomeLocal,
                       String endereco,
                       List<DependenteParadaDTO> listaDependentes) {

    public ParadaDTO(int ordem,
                     TipoParada tipoParada,
                     String nomeLocal,
                     String endereco,
                     List<DependenteParadaDTO> listaDependentes) {
        this(null, ordem, tipoParada, nomeLocal, endereco, listaDependentes);
    }
}
