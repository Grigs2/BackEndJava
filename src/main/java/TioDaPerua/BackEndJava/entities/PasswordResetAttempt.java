package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_attempt", schema = "public", indexes = {
        @Index(name = "idx_password_reset_attempt_email", columnList = "email_fingerprint,solicitado_em"),
        @Index(name = "idx_password_reset_attempt_ip", columnList = "ip_fingerprint,solicitado_em")
})
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_fingerprint", nullable = false, length = 64)
    private String emailFingerprint;

    @Column(name = "ip_fingerprint", nullable = false, length = 64)
    private String ipFingerprint;

    @Column(name = "solicitado_em", nullable = false)
    private Instant solicitadoEm;
}
