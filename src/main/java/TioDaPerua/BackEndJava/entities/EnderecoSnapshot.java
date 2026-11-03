package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class EnderecoSnapshot {

    @Column(length = 8)
    private String cep;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 30)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    public static EnderecoSnapshot de(EnderecoDependente endereco) {
        EnderecoSnapshot snapshot = new EnderecoSnapshot();
        snapshot.setCep(endereco.getCep());
        snapshot.setLogradouro(endereco.getLogradouro());
        snapshot.setNumero(endereco.getNumero());
        snapshot.setComplemento(endereco.getComplemento());
        snapshot.setBairro(endereco.getBairro());
        snapshot.setCidade(endereco.getCidade());
        snapshot.setUf(endereco.getUf());
        return snapshot;
    }

    public String formatar() {
        String complementoTexto = complemento == null || complemento.isBlank() ? "" : ", " + complemento;
        return logradouro + ", " + numero + complementoTexto + " - " + bairro + ", " + cidade + "/" + uf + " - CEP " + cep;
    }
}
