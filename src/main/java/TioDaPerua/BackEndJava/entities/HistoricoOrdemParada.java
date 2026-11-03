package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "historico_ordem_parada", schema = "public")
public class HistoricoOrdemParada implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_historico_ordem_parada",
            sequenceName = "public.seq_historico_ordem_parada", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_historico_ordem_parada")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_dia_id", nullable = false)
    private ViagemDia viagemDia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parada_id", nullable = false)
    private ViagemParada parada;

    @Column(name = "ordem_anterior", nullable = false)
    private int ordemAnterior;

    @Column(name = "ordem_nova", nullable = false)
    private int ordemNova;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_usuario_id", nullable = false)
    private Usuario ator;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;
}
