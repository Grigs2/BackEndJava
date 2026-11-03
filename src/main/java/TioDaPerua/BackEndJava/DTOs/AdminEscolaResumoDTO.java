package TioDaPerua.BackEndJava.DTOs;

public record AdminEscolaResumoDTO(
        Long id,
        String nome,
        String cnpj,
        Long usuarioId,
        boolean contaSuspensa) {
}
