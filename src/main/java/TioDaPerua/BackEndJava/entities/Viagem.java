package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.Periodo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "viagem", schema = "public")
public class Viagem implements Serializable {

    @Serial
    private static final long serialVersionUID = 123L;

    @Id
    @SequenceGenerator(name="SEQ_viagem", sequenceName = "public.seq_viagem", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_viagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo")
    private Periodo periodo;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /** Escolas atendidas pela viagem. Viagens antigas podem não ter escola. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "viagem_escola_destino", schema = "public",
            joinColumns = @JoinColumn(name = "viagem_id"),
            inverseJoinColumns = @JoinColumn(name = "escola_id"))
    private List<Escola> escolas = new ArrayList<>();

    /** Nome personalizado; quando vazio, {@link #getNome()} usa o padrão do período. */
    @Column(name = "nome", length = 200)
    private String nomeCustomizado;

    /** Nome exibido: o personalizado ou o padrão de ida/volta com as escolas atendidas. */
    public String getNome() {
        if (nomeCustomizado != null && !nomeCustomizado.isBlank()) return nomeCustomizado;
        return nomePadrao();
    }

    public void setNome(String nome) {
        this.nomeCustomizado = nome == null || nome.isBlank() ? null : nome.trim();
    }

    /** "Viagem para escola(s): e1, e2" na ida; "Viagem de volta das escola(s): e1, e2" na volta. */
    public String nomePadrao() {
        if (escolas == null || escolas.isEmpty()) return null;
        String nomes = escolas.stream().map(Escola::getNome).filter(Objects::nonNull).collect(Collectors.joining(", "));
        if (nomes.isEmpty()) return null;
        return isVolta() ? "Viagem de volta das escola(s): " + nomes : "Viagem para escola(s): " + nomes;
    }

    private boolean isVolta() {
        return periodo != null && periodo.name().endsWith("_VOLTA");
    }

}
