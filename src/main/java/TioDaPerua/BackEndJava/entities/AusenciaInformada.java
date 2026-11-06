package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Aviso do responsável de que o aluno não vai em uma data. O trecho define quais viagens do dia são afetadas:
 * TODOS (ida e volta), IDA ou VOLTA. Ao preparar a viagem do dia, o aluno já entra como FALTOU.
 */
@Data
@Entity
@Table(name = "ausencia_informada", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uk_ausencia_dependente_data_trecho",
                columnNames = {"dependente_id", "data", "trecho"}))
public class AusenciaInformada implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TODOS = "TODOS";
    public static final String IDA = "IDA";
    public static final String VOLTA = "VOLTA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependente_id", nullable = false)
    private Dependente dependente;

    /** Usuário (responsável) que avisou. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "informada_por_usuario_id", nullable = false)
    private Usuario informadaPor;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    /** TODOS, IDA ou VOLTA (texto, sem CHECK no banco). */
    @Column(name = "trecho", nullable = false, length = 10)
    private String trecho;

    @Column(name = "motivo", length = 200)
    private String motivo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    /** Indica se a ausência vale para uma viagem com o período informado. */
    public boolean valePara(TioDaPerua.BackEndJava.enums.Periodo periodo) {
        if (TODOS.equals(trecho) || periodo == null) return true;
        boolean ida = periodo.name().endsWith("_IDA");
        return IDA.equals(trecho) == ida;
    }
}
