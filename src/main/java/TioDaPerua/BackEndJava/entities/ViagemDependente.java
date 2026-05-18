package TioDaPerua.BackEndJava.entities;

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
@Table(name = "viagem_dependente", schema = "public")
public class ViagemDependente implements Serializable {

    @Serial
    private static final long serialVersionUID = 198L;

    @Id
    @SequenceGenerator(name="SEQ_viagem_dependente", sequenceName = "public.seq_viagem_dependente", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem_dependente")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id")
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id")
    private Dependente dependente;

    @Column(name = "ativo")
    private boolean ativo;

}
