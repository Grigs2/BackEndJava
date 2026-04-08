package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Table(name = "veiculo", schema = "public")
public class Veiculo implements Serializable {

    @Serial
    private static final Long serialVersionUID = 123123123L;

    @Id
    @SequenceGenerator(name="SEQ_veiculo", sequenceName = "public.seq_veiculo", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Veiculo")
    private Long id;

    @Column
    public String modelo;

    @Column(unique = true)
    public String placa;

    @Column
    public int Ano;

    @Column
    public int capacidade;


}
