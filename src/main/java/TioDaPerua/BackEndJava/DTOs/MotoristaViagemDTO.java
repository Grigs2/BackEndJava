package TioDaPerua.BackEndJava.DTOs;

public record MotoristaViagemDTO(Long motoristaId,
                                String nomeMotorista,
                                String telefone,
                                String modeloVeiculo,
                                String placaVeiculo,
                                String corVeiculo,
                                /** Usuário do motorista, para o responsável mandar mensagem. */
                                Long motoristaUsuarioId) {
}
