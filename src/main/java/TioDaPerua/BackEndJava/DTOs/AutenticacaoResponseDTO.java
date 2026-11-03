package TioDaPerua.BackEndJava.DTOs;

public record AutenticacaoResponseDTO(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        PerfilAutenticadoDTO perfil
) {
}
