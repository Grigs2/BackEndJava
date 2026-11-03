package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusValidacaoEndereco;
import TioDaPerua.BackEndJava.enums.TipoOperacaoParada;
import TioDaPerua.BackEndJava.enums.TipoParada;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "viagem_parada", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"viagem_dia_id", "ordem"})
})
public class ViagemParada implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_viagem_parada",
            sequenceName = "public.seq_viagem_parada", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_viagem_parada")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_dia_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ViagemDia viagemDia;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_parada", nullable = false, length = 20)
    private TipoParada tipoParada;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", nullable = false, length = 20)
    private TipoOperacaoParada operacao;

    @Column(name = "nome_local", length = 200)
    private String nomeLocal;

    @Column(name = "endereco", length = 500)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao_endereco", length = 20)
    private StatusValidacaoEndereco statusValidacaoEndereco;

    @OneToMany(mappedBy = "parada", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ViagemParadaParticipante> participantes = new ArrayList<>();
}
