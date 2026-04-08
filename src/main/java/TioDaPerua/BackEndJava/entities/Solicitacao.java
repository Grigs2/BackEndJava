package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitacao", schema = "public")
public class Solicitacao implements Serializable {

    @Serial
    private static final long serialVersionUID = 121387L;

    @Id
    @SequenceGenerator(name="SEQ_solicitacao", sequenceName = "public.seq_solicitacao", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Solicitacao")
    private Long id;

    //Viagem
    //Depedente


    @Column
    private Boolean Respondido;

    @Column
    private Boolean Aceito;

    @Column
    private LocalDateTime DataInicio;

    @Column
    private LocalDateTime DataFim;


    //Responsavel



}
