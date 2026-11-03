package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name = "endereco_operacional", schema = "public")
public class EnderecoOperacional implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_endereco_operacional",
            sequenceName = "public.seq_endereco_operacional", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_endereco_operacional")
    private Long id;

    @Column(name = "cep", nullable = false, length = 8)
    private String cep;

    @Column(name = "logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false, length = 20)
    private StatusValidacaoEndereco statusValidacao;

    public String formatar() {
        String complementoTexto = complemento == null || complemento.isBlank() ? "" : ", " + complemento;
        return logradouro + ", " + numero + complementoTexto + " - " + bairro + ", " + cidade + "/" + uf + " - CEP " + cep;
    }
}
