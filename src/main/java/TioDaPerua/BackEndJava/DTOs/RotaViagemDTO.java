package TioDaPerua.BackEndJava.DTOs;

import java.time.Instant;
import java.util.List;

/**
 * Rota da viagem em andamento, calculada pelo Google a partir da posição atual do veículo.
 *
 * @param disponivel  false quando não há como calcular (sem chave, viagem parada, sem posição); ver {@code mensagem}
 * @param polylines   trechos da rota no formato "encoded polyline" do Google, na ordem
 * @param proximaInstrucao só para o motorista: primeira manobra do trecho atual (ex.: "Vire à direita na Rua X")
 */
public record RotaViagemDTO(
        boolean disponivel,
        String mensagem,
        Instant calculadaEm,
        Double origemLatitude,
        Double origemLongitude,
        List<String> polylines,
        List<ParadaRotaDTO> paradas,
        String proximaInstrucao,
        Long metrosAteInstrucao) {

    public static RotaViagemDTO indisponivel(String mensagem) {
        return new RotaViagemDTO(false, mensagem, Instant.now(), null, null, List.of(), List.of(), null, null);
    }
}
