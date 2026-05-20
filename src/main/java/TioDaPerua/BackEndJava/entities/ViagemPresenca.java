package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusViagemPresenca;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "viagem_presenca", schema = "public", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"dependente_id", "viagem_dia_id"})
})
public class ViagemPresenca implements Serializable {

    @Serial
    private static final long serialVersionUID = 202L;

    @Id
    @SequenceGenerator(name="SEQ_viagem_presenca", sequenceName = "public.seq_viagem_presenca", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem_presenca")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_dia_id")
    private ViagemDia viagemDia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id")
    private Dependente dependente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusViagemPresenca status;

    @Column(name = "horario_embarque")
    private LocalDateTime horarioEmbarque;

    @Column(name = "horario_desembarque")
    private LocalDateTime horarioDesembarque;

}
