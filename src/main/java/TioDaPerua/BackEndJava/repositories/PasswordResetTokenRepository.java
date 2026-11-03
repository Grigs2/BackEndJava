package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT token
        FROM PasswordResetToken token
        JOIN FETCH token.usuario
        WHERE token.tokenHash = :tokenHash
        AND token.usadoEm IS NULL
        AND token.expiraEm > :agora
    """)
    Optional<PasswordResetToken> buscarValidoParaAtualizacao(
            @Param("tokenHash") String tokenHash,
            @Param("agora") Instant agora);

    @Modifying
    @Query("""
        UPDATE PasswordResetToken token
        SET token.usadoEm = :usadoEm
        WHERE token.usuario.id = :usuarioId
        AND token.usadoEm IS NULL
    """)
    int invalidarTokensAtivosDoUsuario(@Param("usuarioId") Long usuarioId,
                                       @Param("usadoEm") Instant usadoEm);

    @Modifying
    @Query("DELETE FROM PasswordResetToken token WHERE token.expiraEm <= :agora OR token.usadoEm IS NOT NULL")
    int removerTokensExpiradosOuUsados(@Param("agora") Instant agora);
}
