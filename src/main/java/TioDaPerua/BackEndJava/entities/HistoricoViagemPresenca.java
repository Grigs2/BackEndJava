package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "historico_viagem_presenca", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uk_historico_presenca_ator_evento",
                columnNames = {"ator_usuario_id", "evento_cliente_id"})
})
public class HistoricoViagemPresenca implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_historico_viagem_presenca",
            sequenceName = "public.seq_historico_viagem_presenca", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_historico_viagem_presenca")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_presenca_id", nullable = false)
    private ViagemPresenca presenca;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusViagemPresenca statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusViagemPresenca statusNovo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_usuario_id", nullable = false)
    private Usuario ator;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(name = "evento_cliente_id")
    private UUID eventoClienteId;

    @Column(name = "instante_dispositivo")
    private Instant instanteDispositivo;

    @Column(name = "recebido_em")
    private Instant recebidoEm;
}
