package TioDaPerua.BackEndJava.enums;

/**
 * Quem iniciou a solicitação de transporte. Quem não iniciou é quem responde:
 * pedido do motorista é respondido pelo responsável e vice-versa.
 * Registros antigos (sem origem) são tratados como MOTORISTA.
 */
public enum OrigemSolicitacao {
    MOTORISTA,
    RESPONSAVEL
}
