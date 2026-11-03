package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.PasswordResetRateLimitKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface PasswordResetRateLimitKeyRepository
        extends JpaRepository<PasswordResetRateLimitKey, String> {

    @Modifying
    @Query(value = """
        INSERT INTO public.password_reset_rate_limit_key (fingerprint, last_seen_em)
        VALUES (:fingerprint, :lastSeenEm)
        ON CONFLICT (fingerprint) DO UPDATE SET last_seen_em = EXCLUDED.last_seen_em
        """, nativeQuery = true)
    int registrarUso(@Param("fingerprint") String fingerprint,
                     @Param("lastSeenEm") Instant lastSeenEm);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT rateKey
        FROM PasswordResetRateLimitKey rateKey
        WHERE rateKey.fingerprint IN :fingerprints
        ORDER BY rateKey.fingerprint
    """)
    List<PasswordResetRateLimitKey> bloquearChaves(@Param("fingerprints") Collection<String> fingerprints);

    long deleteByLastSeenEmBefore(Instant antesDe);
}
