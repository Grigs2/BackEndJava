package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Escola;

/** Resultado da busca de escolas. O id é usado para criar a viagem "Viagem para <escola>". */
public record EscolaBuscaDTO(String nome, String cnpj, Long id) {
    public static EscolaBuscaDTO toDTO(Escola escola) {
        return new EscolaBuscaDTO(escola.getNome(), escola.getCnpj(), escola.getId());
    }
}
