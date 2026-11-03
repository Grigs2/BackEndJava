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
@Table(name = "viagem_escola", schema = "public",
        indexes = @Index(name = "idx_viagem_escola_par_status",
                columnList = "escola_id,motorista_id,status"))
public class ViagemEscola implements Serializable{

    @Serial
    private static final long serialVersionUID = 1928L;


    @Id
    @SequenceGenerator(name="SEQ_viagem_escola", sequenceName = "public.seq_viagem_escola", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem_escola")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escola_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Escola escola;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Motorista motorista;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private StatusVinculoEscola status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_usuario_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario criadoPor;

    @Column(name = "criado_em")
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;
}
