package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT token
        FROM RefreshToken token
        JOIN FETCH token.usuario
        WHERE token.tokenHash = :tokenHash
    """)
    Optional<RefreshToken> buscarPorHashParaAtualizacao(@Param("tokenHash") String tokenHash);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE RefreshToken token
        SET token.revogadoEm = :revogadoEm
        WHERE token.sessionId = :sessionId
        AND token.revogadoEm IS NULL
    """)
    int revogarSessao(@Param("sessionId") UUID sessionId, @Param("revogadoEm") Instant revogadoEm);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE RefreshToken token
        SET token.revogadoEm = :revogadoEm
        WHERE token.usuario.id = :usuarioId
        AND token.revogadoEm IS NULL
    """)
    int revogarSessoesDoUsuario(@Param("usuarioId") Long usuarioId,
                                @Param("revogadoEm") Instant revogadoEm);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        DELETE FROM RefreshToken token
        WHERE token.expiraEm <= :agora
    """)
    int removerTokensExpirados(@Param("agora") Instant agora);
}
