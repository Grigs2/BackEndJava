package TioDaPerua.BackEndJava.enums;

import TioDaPerua.BackEndJava.exceptions.BusinessException;

import java.util.List;

public enum LadoDocumento {
    FRENTE,
    VERSO,
    UNICA;

    public static LadoDocumento de(String valor) {
        try {
            return valueOf(valor.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("Lado da imagem inválido. Use FRENTE, VERSO ou UNICA");
        }
    }

    public static List<LadoDocumento> aceitosPara(TipoDocumento tipo) {
        return tipo == TipoDocumento.VISTORIA ? List.of(UNICA) : List.of(FRENTE, VERSO);
    }
}
