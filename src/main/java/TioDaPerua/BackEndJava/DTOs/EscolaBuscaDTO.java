package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Escola;

public record EscolaBuscaDTO(String nome, String cnpj) {
    public static EscolaBuscaDTO toDTO(Escola escola) {
        return new EscolaBuscaDTO(escola.getNome(), escola.getCnpj());
    }
}
