package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.PasswordResetAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface PasswordResetAttemptRepository extends JpaRepository<PasswordResetAttempt, Long> {

    long countByEmailFingerprintAndSolicitadoEmAfter(String emailFingerprint, Instant depoisDe);

    long countByIpFingerprintAndSolicitadoEmAfter(String ipFingerprint, Instant depoisDe);

    boolean existsByEmailFingerprintAndSolicitadoEmAfter(String emailFingerprint, Instant depoisDe);

    boolean existsByIpFingerprintAndSolicitadoEmAfter(String ipFingerprint, Instant depoisDe);

    long deleteBySolicitadoEmBefore(Instant antesDe);
}
