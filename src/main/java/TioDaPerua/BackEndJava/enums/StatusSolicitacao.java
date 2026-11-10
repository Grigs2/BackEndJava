package TioDaPerua.BackEndJava.enums;

/**
 * Ciclo de vida de uma solicitação de transporte.
 * Registros antigos sem status são tratados pelo campo respondido/aceito.
 */
public enum StatusSolicitacao {
    PENDENTE,
    ACEITA,
    RECUSADA,
    EXPIRADA
}
