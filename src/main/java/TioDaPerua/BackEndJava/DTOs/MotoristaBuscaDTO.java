package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Motorista;

public record MotoristaBuscaDTO(Long id, String nome) {
    public static MotoristaBuscaDTO toDTO(Motorista motorista) {
        return new MotoristaBuscaDTO(motorista.getId(), motorista.getNome());
    }
}
