package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.CategoriaVinculoResponsavel;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "convite_responsavel", schema = "public")
public class ConviteResponsavel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_convite_responsavel",
            sequenceName = "public.seq_convite_responsavel", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_convite_responsavel")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependente_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Dependente dependente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convidado_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Responsavel convidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convidador_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Responsavel convidador;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 30)
    private CategoriaVinculoResponsavel categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusConviteResponsavel status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "respondido_em")
    private Instant respondidoEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "revogado_por_usuario_id")
    private Long revogadoPorUsuarioId;
}
