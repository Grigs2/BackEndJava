package TioDaPerua.BackEndJava.DTOs;

/**
 * Uma conferência automática feita no documento antes da revisão do administrador.
 *
 * @param situacao OK, ATENCAO (o administrador deve conferir com o documento) ou ERRO (impede aprovar)
 */
public record ConferenciaDocumentoDTO(String item, String situacao, String detalhe) {

    public static ConferenciaDocumentoDTO ok(String item, String detalhe) {
        return new ConferenciaDocumentoDTO(item, "OK", detalhe);
    }

    public static ConferenciaDocumentoDTO atencao(String item, String detalhe) {
        return new ConferenciaDocumentoDTO(item, "ATENCAO", detalhe);
    }

    public static ConferenciaDocumentoDTO erro(String item, String detalhe) {
        return new ConferenciaDocumentoDTO(item, "ERRO", detalhe);
    }
}
