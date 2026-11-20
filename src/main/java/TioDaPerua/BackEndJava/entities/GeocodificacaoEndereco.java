package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Cache de endereço → coordenada. Cada endereço é consultado no Google uma vez só;
 * endereços que o Google não achou também ficam guardados (encontrado = false) para não repetir a consulta.
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "geocodificacao_endereco", schema = "public")
public class GeocodificacaoEndereco {

    @Id
    @SequenceGenerator(name = "SEQ_geocodificacao_endereco",
            sequenceName = "public.seq_geocodificacao_endereco", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_geocodificacao_endereco")
    private Long id;

    /** Endereço normalizado (minúsculo, espaços simples). */
    @Column(name = "chave", nullable = false, unique = true, length = 500)
    private String chave;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "encontrado", nullable = false)
    private boolean encontrado;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;
}
