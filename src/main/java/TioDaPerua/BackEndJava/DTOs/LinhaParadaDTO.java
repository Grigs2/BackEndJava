package TioDaPerua.BackEndJava.DTOs;

/**
 * @param acao EMBARQUE ou DESEMBARQUE
 * @param doFilho true somente na parada em que o aluno consultado está aguardando ou a bordo
 * @param rotulo escola para paradas ESCOLA; "Casa de {nome}" na parada do aluno; null nas demais
 */
public record LinhaParadaDTO(
        int ordem,
        String tipoParada,
        String acao,
        boolean concluida,
        boolean doFilho,
        String rotulo) {
}
