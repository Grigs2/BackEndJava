package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "historico_viagem_dia", schema = "public")
public class HistoricoViagemDia implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_historico_viagem_dia",
            sequenceName = "public.seq_historico_viagem_dia", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_historico_viagem_dia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_dia_id", nullable = false)
    private ViagemDia viagemDia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusViagemDia statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusViagemDia statusNovo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_usuario_id", nullable = false)
    private Usuario ator;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(name = "motivo", length = 1000)
    private String motivo;
}
