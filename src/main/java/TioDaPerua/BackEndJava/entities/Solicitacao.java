package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.OrigemSolicitacao;
import TioDaPerua.BackEndJava.enums.StatusSolicitacao;
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

    /** Quem iniciou o pedido; nulo em registros antigos (equivale a MOTORISTA). */
    @Enumerated(EnumType.STRING)
    @Column(name = "origem", length = 20)
    private OrigemSolicitacao origem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private StatusSolicitacao status;

    /** Responsável autorizou que os dados do(s) dependente(s) sejam compartilhados com o motorista se aceitar. */
    @Column(name = "compartilhamento_autorizado")
    private Boolean compartilhamentoAutorizado;

    /** Prazo para resposta (3 dias após a criação). Nulo em registros antigos, que não expiram. */
    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;

    public OrigemSolicitacao getOrigemEfetiva() {
        return origem == null ? OrigemSolicitacao.MOTORISTA : origem;
    }

    public StatusSolicitacao getStatusEfetivo() {
        if (status != null) return status;
        if (!respondido) return StatusSolicitacao.PENDENTE;
        return aceito ? StatusSolicitacao.ACEITA : StatusSolicitacao.RECUSADA;
    }

    public boolean isExpiradaEm(LocalDateTime momento) {
        return !respondido && dataExpiracao != null && momento.isAfter(dataExpiracao);
    }

}
