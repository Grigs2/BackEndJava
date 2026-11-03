package TioDaPerua.BackEndJava.entities;

import TioDaPerua.BackEndJava.enums.AcaoConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.enums.TipoEscopoConsentimentoLocalizacao;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@Table(name = "consentimento_localizacao", schema = "public", indexes = {
        @Index(name = "idx_consentimento_localizacao_scope",
                columnList = "escopo, dependente_id, usuario_id, versao_politica, ocorrido_em"),
        @Index(name = "idx_consentimento_localizacao_ocorrido",
                columnList = "ocorrido_em")
})
public class ConsentimentoLocalizacao {

    @Id
    @SequenceGenerator(name = "SEQ_consentimento_localizacao",
            sequenceName = "public.seq_consentimento_localizacao", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_consentimento_localizacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "escopo", nullable = false, length = 20)
    private TipoEscopoConsentimentoLocalizacao escopo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependente_id")
    @ToString.Exclude
    private Dependente dependente;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 20)
    private AcaoConsentimentoLocalizacao acao;

    @Column(name = "versao_politica", nullable = false, length = 100)
    private String versaoPolitica;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;
}
