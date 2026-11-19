package TioDaPerua.BackEndJava.DTOs;

import java.time.Instant;

/**
 * Parada com a previsão de chegada.
 * Para o motorista vem uma por parada pendente; para o responsável, só a parada de cada filho dele.
 *
 * @param segundos tempo estimado da posição atual do veículo até a parada (com trânsito)
 * @param acao     EMBARQUE ou DESEMBARQUE do aluno nessa parada
 */
public record ParadaRotaDTO(
        Long paradaId,
        Integer ordem,
        String tipoParada,
        String rotulo,
        Double latitude,
        Double longitude,
        long segundos,
        long metros,
        Instant previsaoChegada,
        Long dependenteId,
        String acao) {
}
