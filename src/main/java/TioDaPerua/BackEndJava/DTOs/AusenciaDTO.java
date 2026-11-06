package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.AusenciaInformada;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Ausência avisada pelo responsável. Na criação, envie data, trecho (TODOS, IDA ou VOLTA) e motivo opcional. */
public record AusenciaDTO(Long id,
                          Long dependenteId,
                          @NotNull LocalDate data,
                          String trecho,
                          @Size(max = 200) String motivo) {

    public static AusenciaDTO toDTO(AusenciaInformada a) {
        return new AusenciaDTO(a.getId(), a.getDependente() != null ? a.getDependente().getId() : null,
                a.getData(), a.getTrecho(), a.getMotivo());
    }
}
