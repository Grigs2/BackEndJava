package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Entity
@Table(name = "viagem_escola", schema = "public")
public class ViagemEscola implements Serializable{

    @Serial
    private static final long serialVersionUID = 1928L;


    @Id
    @SequenceGenerator(name="SEQ_viagem_escola", sequenceName = "public.seq_viagem_escola", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Viagem_Escola")
    private Long id;

    //Escola
    //Viagem

}
