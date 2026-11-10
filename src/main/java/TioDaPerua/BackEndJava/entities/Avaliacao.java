package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Nota de um responsável para um motorista com quem ele tem acordo de transporte. Uma por par. */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "avaliacao_motorista", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uk_avaliacao_responsavel_motorista",
                columnNames = {"responsavel_id", "motorista_id"}))
public class Avaliacao implements Serializable {

    @Serial
    private static final long serialVersionUID = 121390L;

    @Id
    @SequenceGenerator(name = "SEQ_avaliacao", sequenceName = "public.seq_avaliacao_motorista", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_avaliacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @Column(name = "nota", nullable = false)
    private Integer nota;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
