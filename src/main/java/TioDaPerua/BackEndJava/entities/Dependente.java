package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dependente", schema = "public")
public class Dependente implements Serializable {

    @Serial
    private static final Long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name="SEQ_dependente", sequenceName = "public.seq_dependente", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Dependente")
    private Long id;

    @Column
    private String nome;

    @Column(unique = true)
    private String cpf;

    @Column
    private LocalDate dataNascimento;

    @Column
    private String periodo;

    @Column
    private String endereco;

    // List<Responsavel>
    @ManyToMany(mappedBy = "dependentes", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Responsavel> responsaveis = new ArrayList<>();


    // Escola

    //List<Viagem>


    public Dependente(Long id, String nome, String cpf, LocalDate dataNascimento, String periodo, String endereco) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.periodo = periodo;
        this.endereco = endereco;

    }

}
