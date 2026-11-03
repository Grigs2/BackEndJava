package TioDaPerua.BackEndJava.DTOs;

public record PerfilAutenticadoDTO(
        Long id,
        Long usuarioId,
        String nome,
        String tipoPerfil
) {
}
