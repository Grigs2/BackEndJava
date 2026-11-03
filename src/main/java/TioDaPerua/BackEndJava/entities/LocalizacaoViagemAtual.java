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
@Table(name = "localizacao_viagem_atual", schema = "public")
public class LocalizacaoViagemAtual {

    @Id
    @SequenceGenerator(name = "SEQ_localizacao_viagem_atual",
            sequenceName = "public.seq_localizacao_viagem_atual", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_localizacao_viagem_atual")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_dia_id", nullable = false, unique = true)
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
