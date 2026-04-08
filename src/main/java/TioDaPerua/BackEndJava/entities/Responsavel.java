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
@Table(name = "responsavel", schema = "public")
public class Responsavel implements Serializable {

    @Serial
    private static final Long serialVersionUID = -12311L;

    @Id
    @SequenceGenerator(name="SEQ_responsavel", sequenceName = "public.seq_responsavel", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_Responsavel")
    private Long id;

    @Column
    private String nome;

    @Column(unique = true)
    private String cpf;

    @Column
    private LocalDate dataNascimento;

    //List<Dependentes>
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(name = "dependente_responsavel", joinColumns = @JoinColumn(name = "responsavel_id"),
                inverseJoinColumns = @JoinColumn(name = "dependente_id"))
    @ToString.Exclude
    private List<Dependente> dependentes = new ArrayList<>();

    //List<Notificacao>

    //List<Solicitacao>

    //Usuario
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;




}
