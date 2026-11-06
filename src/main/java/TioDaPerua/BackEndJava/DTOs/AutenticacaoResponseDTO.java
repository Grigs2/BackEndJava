package TioDaPerua.BackEndJava.DTOs;

/**
 * Resposta de login e renovação. {@code termosPendentes} é true quando a versão vigente dos Termos de Uso e da
 * Política de Privacidade é diferente da que o usuário aceitou: o app pede um novo aceite antes de continuar.
 */
public record AutenticacaoResponseDTO(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn,
        PerfilAutenticadoDTO perfil,
        boolean termosPendentes
) {
}
