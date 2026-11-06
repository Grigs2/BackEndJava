package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

/**
 * Motorista exibido na busca do responsável. Traz só dados públicos do serviço: nome, veículo,
 * bairro/cidade de atuação e viagens ativas (sem CPF, telefone, e-mail ou endereço completo).
 *
 * @param proximidade MESMO_BAIRRO, MESMA_CIDADE, OUTRA_CIDADE ou SEM_REFERENCIA (quando não há endereço para comparar)
 */
public record MotoristaDisponivelDTO(Long id,
                                     String nome,
                                     String veiculoModelo,
                                     String veiculoCor,
                                     Integer veiculoCapacidade,
                                     String bairro,
                                     String cidade,
                                     String uf,
                                     String proximidade,
                                     List<ViagemDTO> viagens) {
}
