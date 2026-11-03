package TioDaPerua.BackEndJava.DTOs;

public record AdminUsuarioResumoDTO(
        Long id,
        String tipoPerfil,
        boolean suspensa,
        String emailMascarado) {
}
