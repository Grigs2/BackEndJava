package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Table(name = "motorista", schema = "public")
public class Motorista implements Serializable {

    @Serial
    private static final Long serialVersionUID = 12312657L;

    @Id
    @SequenceGenerator(name="SEQ_motorista", sequenceName = "public.seq_motorista", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Motorista")
    private Long id;

    @Column
    private String nome;

    @Column
    private LocalDate dataNascimento;

    @Column(unique = true)
    private String cpf;

    @Column
    private String cnh;

    //Veiculo
    @Setter
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    // list<Notificacao>

    // List<viagem>

    // Perfil
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;



    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }
}
