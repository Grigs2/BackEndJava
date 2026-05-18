package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

public record DependenteResumoDTO(Long id,
                                 String nome,
                                 String endereco,
                                 EscolaResumoDTO escola,
                                 List<ResponsavelResumoDTO> responsaveis) {
}
