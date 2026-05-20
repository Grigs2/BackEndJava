package TioDaPerua.BackEndJava.entities;

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
@Table(name = "solicitacao", schema = "public")
public class Solicitacao implements Serializable {

    @Serial
    private static final long serialVersionUID = 121387L;

    @Id
    @SequenceGenerator(name="SEQ_solicitacao", sequenceName = "public.seq_solicitacao", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_solicitacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viagem_id")
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id")
    private Dependente dependente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Responsavel responsavel;

    @Column(name = "respondido")
    private boolean respondido;

    @Column(name = "aceito")
    private boolean aceito;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

}
