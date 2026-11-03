package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name = "viagem_parada_participante", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"parada_id", "viagem_presenca_id"})
})
public class ViagemParadaParticipante implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_viagem_parada_participante",
            sequenceName = "public.seq_viagem_parada_participante", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_viagem_parada_participante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parada_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ViagemParada parada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_presenca_id", nullable = false)
    private ViagemPresenca presenca;

    @Column(name = "nome_dependente_snapshot", length = 200)
    private String nomeDependenteSnapshot;

    @Column(name = "dependente_id_snapshot")
    private Long dependenteIdSnapshot;

    @Column(name = "nome_responsavel_snapshot", length = 200)
    private String nomeResponsavelSnapshot;
}
