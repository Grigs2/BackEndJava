package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista;
import TioDaPerua.BackEndJava.enums.LadoDocumento;

import java.time.Instant;

public record ImagemDocumentoDTO(
        Long documentoId,
        LadoDocumento lado,
        String mimeType,
        Long tamanhoBytes,
        Instant enviadoEm
) {
    public static ImagemDocumentoDTO toDTO(ImagemDocumentoMotorista imagem) {
        return new ImagemDocumentoDTO(
                imagem.getDocumento().getId(),
                imagem.getLado(),
                imagem.getMimeType(),
                imagem.getTamanhoBytes(),
                imagem.getEnviadoEm()
        );
    }
}
