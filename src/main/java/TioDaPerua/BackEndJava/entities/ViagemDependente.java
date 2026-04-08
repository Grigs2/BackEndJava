package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name = "viagem_dependente", schema = "public")
public class ViagemDependente implements Serializable {


    @Serial
    private static final long serialVersionUID = 198L;

    @Id
    @SequenceGenerator(name="SEQ_viagem_dependente", sequenceName = "public.seq_viagem_dependente", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Viagem_Dependente")
    private Long id;

    //Dependete
    //Viagem


}
