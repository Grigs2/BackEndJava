package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ViagemDTO(Long id,
                       @NotNull(message = "ID do motorista está nullo") Long motoristaId,
                       @NotBlank(message = "Período está nullo") String periodo,
                       Boolean ativo) {

    public static ViagemDTO toDTO(Viagem viagem) {
        if (viagem == null) return null;
        return new ViagemDTO(
                viagem.getId(),
                viagem.getMotorista().getId(),
                viagem.getPeriodo().name(),
                viagem.getAtivo() != null ? viagem.getAtivo() : Boolean.FALSE
        );
    }

    public static Viagem toEntity(ViagemDTO dto) {
        Viagem viagem = new Viagem();
        viagem.setId(dto.id());
        viagem.setPeriodo(Periodo.valueOf(dto.periodo()));
        if (dto.ativo() != null) {
            viagem.setAtivo(dto.ativo());
        }
        return viagem;
    }
}
