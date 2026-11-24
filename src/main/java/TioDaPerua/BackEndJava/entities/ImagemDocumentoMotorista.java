package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.LadoDocumento;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "imagem_documento_motorista", schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uk_imagem_documento_lado", columnNames = {"documento_id", "lado"}))
public class ImagemDocumentoMotorista implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "SEQ_imagem_documento_motorista", sequenceName = "public.seq_imagem_documento_motorista", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_imagem_documento_motorista")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoMotorista documento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LadoDocumento lado;

    @Column(nullable = false, length = 500)
    private String caminho;

    @Column(nullable = false, length = 30)
    private String mimeType;

    @Column(nullable = false)
    private Long tamanhoBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    private Instant enviadoEm;
}
