package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.EnderecoSnapshot;

public record EnderecoSnapshotDTO(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf) {

    public static EnderecoSnapshotDTO toDTO(EnderecoSnapshot endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoSnapshotDTO(
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf());
    }
}
