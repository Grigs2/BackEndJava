package TioDaPerua.BackEndJava.validation;

import TioDaPerua.BackEndJava.exceptions.BusinessException;

import java.nio.charset.StandardCharsets;

public final class SenhaPolicy {
    private static final String MENSAGEM =
            "A senha deve conter ao menos 12 caracteres e no máximo 72 bytes UTF-8";

    private SenhaPolicy() {
    }

    public static boolean valida(String senha) {
        if (senha == null || senha.isBlank()) {
            return false;
        }
        return senha.codePointCount(0, senha.length()) >= 12
                && senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public static void exigirValida(String senha) {
        if (!valida(senha)) {
            throw new BusinessException(MENSAGEM);
        }
    }
}
