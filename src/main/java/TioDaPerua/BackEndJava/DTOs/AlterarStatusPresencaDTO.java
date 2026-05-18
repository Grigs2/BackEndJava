package TioDaPerua.BackEndJava.DTOs;

public record AlterarStatusPresencaDTO(
    Long idViagemDia,
    Long idDependente,
    String novoStatus
) {}
