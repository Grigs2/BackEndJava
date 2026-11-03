package TioDaPerua.BackEndJava.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "password_reset_rate_limit_key", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetRateLimitKey {

    @Id
    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    @Column(name = "last_seen_em", nullable = false)
    private Instant lastSeenEm;
}
