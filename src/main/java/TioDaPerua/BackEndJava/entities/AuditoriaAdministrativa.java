package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "auditoria_administrativa", schema = "public", indexes = {
        @Index(name = "idx_auditoria_adm_ocorrido_em", columnList = "ocorrido_em"),
        @Index(name = "idx_auditoria_adm_ator_acao", columnList = "ator_usuario_id,acao")
})
public class AuditoriaAdministrativa implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_auditoria_administrativa",
            sequenceName = "public.seq_auditoria_administrativa", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_auditoria_administrativa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_usuario_id", nullable = false)
    private Usuario ator;

    @Column(name = "acao", nullable = false, length = 60)
    private String acao;

    @Column(name = "tipo_alvo", nullable = false, length = 60)
    private String tipoAlvo;

    @Column(name = "alvo_id")
    private Long alvoId;

    @Column(name = "resultado", nullable = false, length = 30)
    private String resultado;

    @Column(name = "motivo", length = 1000)
    private String motivo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;
}
