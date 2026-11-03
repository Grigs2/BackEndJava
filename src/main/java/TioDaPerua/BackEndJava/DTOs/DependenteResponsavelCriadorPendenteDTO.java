package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Dependente;

import java.util.List;

public record DependenteResponsavelCriadorPendenteDTO(
        Long dependenteId,
        String dependenteNome,
        List<ResponsavelResumoDTO> responsaveis) {

    public static DependenteResponsavelCriadorPendenteDTO toDTO(Dependente dependente) {
        return new DependenteResponsavelCriadorPendenteDTO(
                dependente.getId(),
                dependente.getNome(),
                dependente.getResponsaveis().stream()
                        .map(responsavel -> new ResponsavelResumoDTO(
                                responsavel.getId(),
                                responsavel.getNome(),
                                responsavel.getUsuario() != null
                                        ? responsavel.getUsuario().getTelefone() : null))
                        .toList());
    }
}
