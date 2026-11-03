package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ConviteResponsavel;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConviteResponsavelRepository extends JpaRepository<ConviteResponsavel, Long> {

    List<ConviteResponsavel> findByDependenteIdOrderByCriadoEmDesc(Long dependenteId);

    List<ConviteResponsavel> findByDependenteIdAndConvidadoIdOrderByCriadoEmDesc(
            Long dependenteId, Long convidadoId);

    List<ConviteResponsavel> findByConvidadoIdAndStatusOrderByCriadoEmDesc(
            Long convidadoId, StatusConviteResponsavel status);

    @Query("""
        SELECT convite
        FROM ConviteResponsavel convite
        WHERE convite.convidado.usuario.id = :usuarioId
        AND convite.status = :status
        AND convite.expiraEm > :agora
        ORDER BY convite.criadoEm DESC
        """)
    List<ConviteResponsavel> listarPendentesDoUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("status") StatusConviteResponsavel status,
            @Param("agora") Instant agora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT convite
        FROM ConviteResponsavel convite
        WHERE convite.id = :conviteId
        AND convite.convidado.usuario.id = :usuarioId
        """)
    Optional<ConviteResponsavel> buscarParaResposta(
            @Param("conviteId") Long conviteId,
            @Param("usuarioId") Long usuarioId);

    @Query("""
        SELECT convite
        FROM ConviteResponsavel convite
        WHERE convite.dependente.id = :dependenteId
        AND convite.convidado.id = :convidadoId
        AND convite.status = :status
        ORDER BY convite.criadoEm DESC
        """)
    List<ConviteResponsavel> buscarPorDependenteEConvidadoEStatus(
            @Param("dependenteId") Long dependenteId,
            @Param("convidadoId") Long convidadoId,
            @Param("status") StatusConviteResponsavel status);

    @Modifying
    @Query("""
        UPDATE ConviteResponsavel convite
        SET convite.status = :expirado
        WHERE convite.dependente.id = :dependenteId
        AND convite.convidado.id = :convidadoId
        AND convite.status = :pendente
        AND convite.expiraEm <= :agora
        """)
    int expirarConvitesVencidos(
            @Param("dependenteId") Long dependenteId,
            @Param("convidadoId") Long convidadoId,
            @Param("pendente") StatusConviteResponsavel pendente,
            @Param("expirado") StatusConviteResponsavel expirado,
            @Param("agora") Instant agora);

    @Query("""
        SELECT convite
        FROM ConviteResponsavel convite
        WHERE convite.dependente.id = :dependenteId
        AND convite.convidado.id = :convidadoId
        AND convite.status = :status
        ORDER BY convite.respondidoEm DESC
        """)
    List<ConviteResponsavel> buscarAceitosDoVinculo(
            @Param("dependenteId") Long dependenteId,
            @Param("convidadoId") Long convidadoId,
            @Param("status") StatusConviteResponsavel status);
}
