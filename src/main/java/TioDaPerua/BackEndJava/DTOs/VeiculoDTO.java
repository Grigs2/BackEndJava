package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Veiculo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.io.Serial;
import java.io.Serializable;

public record VeiculoDTO(Long id,
                         @NotBlank String modelo,
                         @NotBlank String placa,
                         @Min(1900) int ano,
                         @Positive int capacidade,
                         String cor) implements Serializable {


    @Serial
    private static final long serialVersionUID = 18394L;

    public static VeiculoDTO toDTO(Veiculo veiculo) {
        if (veiculo == null) return null;
        return new VeiculoDTO(veiculo.getId(), veiculo.getModelo(), veiculo.getPlaca(), veiculo.getAno(), veiculo.getCapacidade(), veiculo.getCor());
    }

    public static Veiculo toEntity(VeiculoDTO dto) {
        if (dto == null) return null;
        return new Veiculo(dto.id(), dto.modelo(), dto.placa(), dto.ano(), dto.capacidade(), dto.cor());
    }
}
