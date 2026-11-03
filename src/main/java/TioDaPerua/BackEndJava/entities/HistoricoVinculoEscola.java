package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "historico_vinculo_escola", schema = "public",
        indexes = @Index(name = "idx_historico_vinculo_ocorrido",
                columnList = "vinculo_id,ocorrido_em"))
public class HistoricoVinculoEscola implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_historico_vinculo_escola",
            sequenceName = "public.seq_historico_vinculo_escola", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_historico_vinculo_escola")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculo_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ViagemEscola vinculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusVinculoEscola statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusVinculoEscola statusNovo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario ator;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;
}
