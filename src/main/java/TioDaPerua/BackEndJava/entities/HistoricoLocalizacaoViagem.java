package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@Table(name = "historico_localizacao_viagem", schema = "public", indexes = {
        @Index(name = "idx_historico_localizacao_execucao_recebido",
                columnList = "viagem_dia_id, recebido_em, id")
})
public class HistoricoLocalizacaoViagem {

    @Id
    @SequenceGenerator(name = "SEQ_historico_localizacao_viagem",
            sequenceName = "public.seq_historico_localizacao_viagem", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_historico_localizacao_viagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_dia_id", nullable = false)
    @ToString.Exclude
    private ViagemDia viagemDia;

    @Column(name = "latitude", nullable = false, precision = 8, scale = 5)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 8, scale = 5)
    private BigDecimal longitude;

    @Column(name = "instante_dispositivo", nullable = false)
    private Instant instanteDispositivo;

    @Column(name = "recebido_em", nullable = false)
    private Instant recebidoEm;
}
