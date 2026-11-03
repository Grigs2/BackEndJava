package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.EnderecoDependente;

import java.time.Instant;

public record EnderecoDependenteDTO(
        Long id,
        Long dependenteId,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String finalidade,
        String statusValidacao,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm) {

    public static EnderecoDependenteDTO toDTO(EnderecoDependente endereco) {
        return new EnderecoDependenteDTO(
                endereco.getId(),
                endereco.getDependente().getId(),
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                endereco.getFinalidade().name(),
                endereco.getStatusValidacao().name(),
                endereco.isAtivo(),
                endereco.getCriadoEm(),
                endereco.getAtualizadoEm());
    }
}
