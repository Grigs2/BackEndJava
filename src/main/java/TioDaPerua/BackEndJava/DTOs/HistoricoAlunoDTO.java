package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Uma viagem do dia na visão do responsável: o que aconteceu com o aluno, quando e com quem. */
public record HistoricoAlunoDTO(Long viagemDiaId,
                                LocalDate data,
                                String viagemNome,
                                String periodo,
                                String motoristaNome,
                                String placa,
                                String statusPresenca,
                                LocalDateTime horarioEmbarque,
                                LocalDateTime horarioDesembarque,
                                String statusViagem,
                                String motivo) {
}
