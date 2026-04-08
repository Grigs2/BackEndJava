package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Veiculo;

import java.io.Serial;
import java.io.Serializable;

public record VeiculoDTO(Long id,
                         String modelo,
                         String placa,
                         int ano,
                         int capacidade) implements Serializable {


    @Serial
    private static final long serialVersionUID = 18394L;

    public static VeiculoDTO toDTO(Veiculo veiculo) {
        if (veiculo == null) return null;
        return new VeiculoDTO(veiculo.getId(), veiculo.getModelo(), veiculo.getPlaca(), veiculo.getAno(), veiculo.getCapacidade());
    }

    public static Veiculo toEntity(VeiculoDTO dto) {
        if (dto == null) return null;
        return new Veiculo(dto.id(), dto.modelo(), dto.placa(), dto.ano(), dto.capacidade());
    }
}
