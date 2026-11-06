package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Viagem do dia no histórico do motorista. Mantém os campos de {@link ViagemDiaDTO} (id, data, status,
 * dataUltimaAlteracaoStatus) e acrescenta nome, período, contagens, início, fim e motivo de cancelamento.
 */
public record ViagemDiaResumoDTO(Long id,
                                 LocalDate data,
                                 String status,
                                 LocalDateTime dataUltimaAlteracaoStatus,
                                 Long viagemId,
                                 String viagemNome,
                                 String periodo,
                                 int totalAlunos,
                                 int embarcados,
                                 int desembarcados,
                                 int faltas,
                                 LocalDateTime inicio,
                                 LocalDateTime fim,
                                 String motivo) {
}
