package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/** Aparelho que recebe push (token do Expo). Desativado ao sair da conta ou quando o Expo informa token inválido. */
@Data
@Entity
@Table(name = "dispositivo_push", schema = "public")
public class DispositivoPush implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expo_token", nullable = false, unique = true, length = 200)
    private String expoToken;

    /** ios, android ou web (informativo). */
    @Column(name = "plataforma", length = 20)
    private String plataforma;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "ultimo_uso_em")
    private Instant ultimoUsoEm;
}
