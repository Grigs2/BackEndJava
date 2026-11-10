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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator= "SEQ_notificacao")
    private Long id;

    @Column
    private String titulo;

    @Column
    private String mensagem;

    @Column
    private LocalDateTime data;

    @Column
    private Boolean visto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remetente_id")
    private Usuario remetente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;

    /** {@link TioDaPerua.BackEndJava.enums.TipoNotificacao} gravado como texto; nulo em registros antigos (= GERAL). */
    @Column(name = "tipo", length = 40)
    private String tipo;

    /** NORMAL ou ALTA. */
    @Column(name = "prioridade", length = 10)
    private String prioridade;

    /** Viagem do dia relacionada (abre o detalhe no app). */
    @Column(name = "viagem_dia_id")
    private Long viagemDiaId;

    /** Aluno relacionado. */
    @Column(name = "dependente_id")
    private Long dependenteId;

    /** Momento da leitura; {@code visto} continua existindo por compatibilidade. */
    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    /** Evita aviso duplicado para o mesmo evento e destinatário. */
    @Column(name = "chave_dedup", length = 160, unique = true)
    private String chaveDedup;

}
