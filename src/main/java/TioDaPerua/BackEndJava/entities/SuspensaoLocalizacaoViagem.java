package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@Table(name = "suspensao_localizacao_viagem", schema = "public")
public class SuspensaoLocalizacaoViagem {

    @Id
    @SequenceGenerator(name = "SEQ_suspensao_localizacao_viagem",
            sequenceName = "public.seq_suspensao_localizacao_viagem", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_suspensao_localizacao_viagem")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viagem_dia_id", nullable = false, unique = true)
    @ToString.Exclude
    private ViagemDia viagemDia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario ator;

    @Column(name = "suspenso_em", nullable = false)
    private Instant suspensoEm;
}
