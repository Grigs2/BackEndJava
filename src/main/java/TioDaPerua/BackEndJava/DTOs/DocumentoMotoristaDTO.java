package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;

import java.time.Instant;
import java.time.LocalDate;

public record DocumentoMotoristaDTO(
        Long id,
        TipoDocumento tipo,
        String numero,
        String categoria,
        LocalDate dataRealizacao,
        LocalDate dataValidade,
        StatusDocumento status,
        Instant submetidoEm,
        Instant substituidoEm,
        Instant revisadoEm,
        Long revisadoPorId,
        String motivoRecusa
) {
    public static DocumentoMotoristaDTO toDTO(DocumentoMotorista documento) {
        StatusDocumento status = documento.getStatus();
        if (status == StatusDocumento.APROVADO
                && documento.getDataValidade() != null
                && documento.getDataValidade().isBefore(LocalDate.now())) {
            status = StatusDocumento.VENCIDO;
        }
        return new DocumentoMotoristaDTO(
                documento.getId(),
                documento.getTipo(),
                documento.getNumero(),
                documento.getCategoria(),
                documento.getDataRealizacao(),
                documento.getDataValidade(),
                status,
                documento.getSubmetidoEm(),
                documento.getSubstituidoEm(),
                documento.getRevisadoEm(),
                documento.getRevisadoPor() == null ? null : documento.getRevisadoPor().getId(),
                documento.getMotivoRecusa()
        );
    }
}
