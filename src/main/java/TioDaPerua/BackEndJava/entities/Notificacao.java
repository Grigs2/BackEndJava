package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notificacao", schema = "public")
public class Notificacao implements Serializable {
    @Serial
    private static final long serialVersionUID = -9081L;

    @Id
    @SequenceGenerator(name="SEQ_notificacao", sequenceName = "public.seq_notificacao", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Notificacao")
    private Long id;

    @Column
    private String titulo;

    @Column
    private String mensagem;

    @Column
    private LocalDateTime data;

    @Column
    private Boolean visto;

    //Rementente Usuario
    //Destinatario Usuario

}
