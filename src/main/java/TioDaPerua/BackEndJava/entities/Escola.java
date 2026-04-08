package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
@Entity
@Table(name = "escola", schema = "public")
public class Escola implements Serializable  {

    @Serial
    private static final Long serialVersionUID = 1236783L;

    @Id
    @SequenceGenerator(name="SEQ_escola", sequenceName = "public.seq_escola", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Escola")
    private Long id;

    @Column
    private String nome;

    @Column
    private String admResponsavel;

    // List<Dependentes>

    // Perfil



}
