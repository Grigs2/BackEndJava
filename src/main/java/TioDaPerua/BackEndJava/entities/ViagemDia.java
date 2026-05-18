package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "viagem_dia", schema = "public", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"viagem_id", "data"})
})
public class ViagemDia implements Serializable {

    @Serial
    private static final long serialVersionUID = 201L;

    @Id
    @SequenceGenerator(name="SEQ_viagem_dia", sequenceName = "public.seq_viagem_dia", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem_dia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id")
    private Viagem viagem;

    @Column(name = "data")
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusViagemDia status;

    @Column(name = "data_ultima_alteracao_status")
    private LocalDateTime dataUltimaAlteracaoStatus;

}
