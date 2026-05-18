package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.Periodo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "viagem", schema = "public")
public class Viagem implements Serializable {

    @Serial
    private static final long serialVersionUID = 123L;

    @Id
    @SequenceGenerator(name="SEQ_viagem", sequenceName = "public.seq_viagem", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo")
    private Periodo periodo;

}
