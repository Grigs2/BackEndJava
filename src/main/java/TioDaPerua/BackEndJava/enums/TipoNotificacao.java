package TioDaPerua.BackEndJava.enums;

/**
 * Tipo da notificação. É gravado como texto (coluna sem restrição CHECK) para que novos tipos não exijam
 * ajuste de banco. O grupo é usado nos filtros da central de notificações do app.
 */
public enum TipoNotificacao {
    MOTORISTA_A_CAMINHO(Grupo.VIAGEM, Prioridade.NORMAL),
    EMBARQUE(Grupo.VIAGEM, Prioridade.NORMAL),
    DESEMBARQUE(Grupo.VIAGEM, Prioridade.NORMAL),
    VIAGEM_FINALIZADA(Grupo.VIAGEM, Prioridade.NORMAL),
    AUSENCIA(Grupo.ALERTA, Prioridade.ALTA),
    AUSENCIA_INFORMADA(Grupo.ALERTA, Prioridade.NORMAL),
    ATRASO(Grupo.ALERTA, Prioridade.ALTA),
    VIAGEM_CANCELADA(Grupo.ALERTA, Prioridade.ALTA),
    VIAGEM_INTERROMPIDA(Grupo.ALERTA, Prioridade.ALTA),
    TROCA_VEICULO(Grupo.ALERTA, Prioridade.ALTA),
    SOLICITACAO(Grupo.SOLICITACAO, Prioridade.NORMAL),
    MANUAL(Grupo.AVISO, Prioridade.NORMAL),
    GERAL(Grupo.AVISO, Prioridade.NORMAL);

    public enum Grupo { VIAGEM, ALERTA, SOLICITACAO, AVISO }

    public enum Prioridade { NORMAL, ALTA }

    private final Grupo grupo;
    private final Prioridade prioridade;

    TipoNotificacao(Grupo grupo, Prioridade prioridade) {
        this.grupo = grupo;
        this.prioridade = prioridade;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public static java.util.List<String> nomesDoGrupo(Grupo grupo) {
        return java.util.Arrays.stream(values()).filter(t -> t.grupo == grupo).map(Enum::name).toList();
    }
}
