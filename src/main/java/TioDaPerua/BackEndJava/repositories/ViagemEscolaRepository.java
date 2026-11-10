package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ViagemEscola;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface ViagemEscolaRepository extends JpaRepository<ViagemEscola, Long> {
    boolean existsByEscola_IdAndMotorista_IdAndStatusIn(
            Long escolaId, Long motoristaId, Collection<StatusVinculoEscola> statuses);

    @Query("""
        SELECT DISTINCT v.escola.id FROM ViagemEscola v
        WHERE v.motorista.id = :motoristaId
        AND v.escola.id IN :escolaIds
        AND v.status = :status
    """)
    List<Long> listarIdsEscolasAtivasDoMotorista(
            @Param("motoristaId") Long motoristaId,
            @Param("escolaIds") Collection<Long> escolaIds,
            @Param("status") StatusVinculoEscola status);

    /** Motoristas com vínculo ATIVO com pelo menos uma das escolas informadas. */
    @Query("""
        SELECT DISTINCT v.motorista.id FROM ViagemEscola v
        WHERE v.escola.id IN :escolaIds
        AND v.status = :status
    """)
    List<Long> listarIdsMotoristasAtivosDasEscolas(
            @Param("escolaIds") Collection<Long> escolaIds,
            @Param("status") StatusVinculoEscola status);

    @Query("SELECT v FROM ViagemEscola v JOIN FETCH v.escola JOIN FETCH v.motorista "
            + "WHERE v.motorista.id = :motoristaId AND v.status IS NOT NULL ORDER BY v.criadoEm DESC")
    List<ViagemEscola> listarDoMotorista(@Param("motoristaId") Long motoristaId);

    @Query("SELECT v FROM ViagemEscola v JOIN FETCH v.escola JOIN FETCH v.motorista "
            + "WHERE v.motorista.id = :motoristaId AND v.status = :status ORDER BY v.criadoEm DESC")
    List<ViagemEscola> listarDoMotoristaPorStatus(
            @Param("motoristaId") Long motoristaId, @Param("status") StatusVinculoEscola status);

    @Query("SELECT v FROM ViagemEscola v JOIN FETCH v.escola JOIN FETCH v.motorista "
            + "WHERE v.escola.id = :escolaId AND v.status IS NOT NULL ORDER BY v.criadoEm DESC")
    List<ViagemEscola> listarDaEscola(@Param("escolaId") Long escolaId);

    @Query("SELECT v FROM ViagemEscola v JOIN FETCH v.escola JOIN FETCH v.motorista "
            + "WHERE v.escola.id = :escolaId AND v.status = :status ORDER BY v.criadoEm DESC")
    List<ViagemEscola> listarDaEscolaPorStatus(
            @Param("escolaId") Long escolaId, @Param("status") StatusVinculoEscola status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ViagemEscola v WHERE v.id = :id")
    Optional<ViagemEscola> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT vinculo
        FROM ViagemEscola vinculo
        WHERE (:escolaId IS NULL OR vinculo.escola.id = :escolaId)
        AND (:motoristaId IS NULL OR vinculo.motorista.id = :motoristaId)
        AND (:status IS NULL OR vinculo.status = :status)
        ORDER BY vinculo.criadoEm DESC, vinculo.id DESC
        """)
    Page<ViagemEscola> pesquisarAdministrativamente(
            @Param("escolaId") Long escolaId,
            @Param("motoristaId") Long motoristaId,
            @Param("status") StatusVinculoEscola status,
            Pageable pageable);
}
