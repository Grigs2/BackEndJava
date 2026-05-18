package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;

public record ViagemDTO(Long id,
                       Long motoristaId,
                       String periodo) {

    public static ViagemDTO toDTO(Viagem viagem) {
        if (viagem == null) return null;
        return new ViagemDTO(
                viagem.getId(),
                viagem.getMotorista() != null ? viagem.getMotorista().getId() : null,
                viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null
        );
    }

    public static Viagem toEntity(ViagemDTO dto) {
        if (dto == null) return null;
        Viagem viagem = new Viagem();
        viagem.setId(dto.id());
        if (dto.periodo() != null) {
            viagem.setPeriodo(Periodo.valueOf(dto.periodo()));
        }
        return viagem;
    }
}
