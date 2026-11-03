package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "documento_motorista", schema = "public",
        indexes = @Index(name = "idx_documento_motorista_tipo_id", columnList = "motorista_id,tipo,id"))
public class DocumentoMotorista implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_documento_motorista", sequenceName = "public.seq_documento_motorista", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_documento_motorista")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDocumento tipo;

    @Column(length = 100)
    private String numero;

    @Column(length = 10)
    private String categoria;

    private LocalDate dataRealizacao;

    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDocumento status = StatusDocumento.PENDENTE;

    @Column(nullable = false)
    private Instant submetidoEm;

    private Instant substituidoEm;

    private Instant revisadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private Administrador revisadoPor;

    @Column(length = 1000)
    private String motivoRecusa;
}
