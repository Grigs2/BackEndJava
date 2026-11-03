package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "endereco_dependente", schema = "public")
public class EnderecoDependente implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_endereco_dependente",
            sequenceName = "public.seq_endereco_dependente", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_endereco_dependente")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependente_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Dependente dependente;

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
    @Column(name = "finalidade", nullable = false, length = 20)
    private FinalidadeEndereco finalidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false, length = 20)
    private StatusValidacaoEndereco statusValidacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
