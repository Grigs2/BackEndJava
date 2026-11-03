package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ViagemEscola;

import java.time.Instant;

public record VinculoEscolaDTO(
        Long id,
        Long escolaId,
        String escolaNome,
        String escolaCnpj,
        Long motoristaId,
        String motoristaNome,
        String status,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static VinculoEscolaDTO toDTO(ViagemEscola vinculo) {
        return new VinculoEscolaDTO(
                vinculo.getId(),
                vinculo.getEscola().getId(),
                vinculo.getEscola().getNome(),
                vinculo.getEscola().getCnpj(),
                vinculo.getMotorista().getId(),
                vinculo.getMotorista().getNome(),
                vinculo.getStatus().name(),
                vinculo.getCriadoEm(),
                vinculo.getAtualizadoEm());
    }
}
