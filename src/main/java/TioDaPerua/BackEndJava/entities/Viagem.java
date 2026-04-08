package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name = "viagem", schema = "public")
public class Viagem implements Serializable {

    @Serial
    private static final long serialVersionUID = 123L;

    @Id
    @SequenceGenerator(name="SEQ_viagem", sequenceName = "public.seq_viagem", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Viagem")
    private Long id;

    //List<Escola>
    //List<Dependentes>
    //List<Solitacao>
    //Motorista


    @Column
    private String periodo;



}
