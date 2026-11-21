package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

/**
 * Linha de paradas da viagem para o responsável acompanhar o aluno. Não contém endereços nem coordenadas.
 *
 * @param indiceAtual ordem da primeira parada com aluno pendente; null antes de sair ou após concluir
 */
public record LinhaViagemDTO(
        boolean disponivel,
        String mensagem,
        Integer indiceAtual,
        List<LinhaParadaDTO> paradas) {

    public static LinhaViagemDTO indisponivel(String mensagem) {
        return new LinhaViagemDTO(false, mensagem, null, List.of());
    }
}
