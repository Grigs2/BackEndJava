package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "viagem_presenca", schema = "public", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"dependente_id", "viagem_dia_id"})
})
public class ViagemPresenca implements Serializable {

    @Serial
    private static final long serialVersionUID = 202L;

    @Id
    @SequenceGenerator(name="SEQ_viagem_presenca", sequenceName = "public.seq_viagem_presenca", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem_presenca")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_dia_id")
    private ViagemDia viagemDia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id")
    private Dependente dependente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusViagemPresenca status;

    @Column(name = "horario_embarque")
    private LocalDateTime horarioEmbarque;

    @Column(name = "horario_desembarque")
    private LocalDateTime horarioDesembarque;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "cep", column = @Column(name = "embarque_cep", length = 8)),
        @AttributeOverride(name = "logradouro", column = @Column(name = "embarque_logradouro", length = 150)),
        @AttributeOverride(name = "numero", column = @Column(name = "embarque_numero", length = 30)),
        @AttributeOverride(name = "complemento", column = @Column(name = "embarque_complemento", length = 100)),
        @AttributeOverride(name = "bairro", column = @Column(name = "embarque_bairro", length = 100)),
        @AttributeOverride(name = "cidade", column = @Column(name = "embarque_cidade", length = 100)),
        @AttributeOverride(name = "uf", column = @Column(name = "embarque_uf", length = 2))
    })
    private EnderecoSnapshot enderecoEmbarqueSnapshot;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "cep", column = @Column(name = "desembarque_cep", length = 8)),
        @AttributeOverride(name = "logradouro", column = @Column(name = "desembarque_logradouro", length = 150)),
        @AttributeOverride(name = "numero", column = @Column(name = "desembarque_numero", length = 30)),
        @AttributeOverride(name = "complemento", column = @Column(name = "desembarque_complemento", length = 100)),
        @AttributeOverride(name = "bairro", column = @Column(name = "desembarque_bairro", length = 100)),
        @AttributeOverride(name = "cidade", column = @Column(name = "desembarque_cidade", length = 100)),
        @AttributeOverride(name = "uf", column = @Column(name = "desembarque_uf", length = 2))
    })
    private EnderecoSnapshot enderecoDesembarqueSnapshot;

    @Column(name = "embarque_endereco_legado_snapshot", length = 500)
    private String enderecoEmbarqueLegadoSnapshot;

    @Column(name = "desembarque_endereco_legado_snapshot", length = 500)
    private String enderecoDesembarqueLegadoSnapshot;

}
