package TioDaPerua.BackEndJava.DTOs;

import java.io.Serial;
import java.io.Serializable;

import TioDaPerua.BackEndJava.entities.Veiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VeiculoDTO(Long id,
                         @NotBlank(message = "Modelo está nullo")String modelo,
                         @NotBlank(message = "Placa está nullo")
                            @Pattern(regexp = "^[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}$", message = "A placa deve estar no formato (ABC1D23).")String placa,
                         @NotBlank(message = "Ano está nullo")
                            @Min(value = 1900, message = "O ano do veículo deve ser maior que 1900.")
                            @Max(value = 2027, message = "O ano do veículo não pode ser superior ao ano modelo atual.")int ano,
                         @NotBlank(message = "Capacidade está nullo")int capacidade,
                         @NotBlank(message = "Cor está nullo")String cor) implements Serializable {


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
