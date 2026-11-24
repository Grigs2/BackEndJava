package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

/**
 * Documento pendente com o que o administrador precisa para decidir: de quem é, de qual veículo,
 * o resultado das conferências automáticas e as imagens enviadas.
 */
public record DocumentoRevisaoDTO(
        DocumentoMotoristaDTO documento,
        Long motoristaId,
        String motoristaNome,
        Integer motoristaIdade,
        String veiculoPlaca,
        String veiculoModelo,
        List<ConferenciaDocumentoDTO> conferencias,
        boolean podeAprovar,
        List<ImagemDocumentoDTO> imagens) {
}
