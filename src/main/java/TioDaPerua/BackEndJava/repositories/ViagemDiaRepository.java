package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ViagemDia;
import TioDaPerua.BackEndJava.enums.StatusViagemDia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemDiaRepository extends JpaRepository<ViagemDia, Long> {

    @Query("SELECT vd FROM ViagemDia vd WHERE vd.viagem.id = :viagemId AND vd.data = :data")
    Optional<ViagemDia> findByViagemIdAndData(Long viagemId, LocalDate data);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vd FROM ViagemDia vd WHERE vd.id = :id")
    Optional<ViagemDia> findByIdForUpdate(Long id);

    @Query("SELECT vd FROM ViagemDia vd WHERE vd.viagem.motorista.id = :motoristaId ORDER BY vd.id DESC")
    List<ViagemDia> findByMotoristaId(Long motoristaId);

    @Query("""
        SELECT vd FROM ViagemDia vd
        WHERE vd.viagem.motorista.id = :motoristaId
        AND vd.data BETWEEN :inicio AND :fim
        ORDER BY vd.data ASC, vd.viagem.id ASC
    """)
    List<ViagemDia> findByMotoristaIdAndDataBetween(Long motoristaId, LocalDate inicio, LocalDate fim);

    boolean existsByIdAndViagem_Motorista_Usuario_Id(Long viagemDiaId, Long usuarioId);

    @Query("""
        SELECT vd.id FROM ViagemDia vd
        WHERE vd.viagem.motorista.usuario.id = :usuarioId
        AND vd.status = :status
    """)
    List<Long> findIdsByMotoristaUsuarioIdAndStatus(Long usuarioId, StatusViagemDia status);

    @Query("""
        SELECT DISTINCT vd.id FROM ViagemDia vd
        JOIN ViagemPresenca presenca ON presenca.viagemDia.id = vd.id
        WHERE presenca.dependente.id = :dependenteId
        AND vd.status = :status
    """)
    List<Long> findIdsByDependenteIdAndStatus(Long dependenteId, StatusViagemDia status);

    @Query("""
        SELECT CASE WHEN COUNT(presenca) > 0 THEN true ELSE false END
        FROM ViagemPresenca presenca
        JOIN presenca.dependente.responsaveis responsavel
        WHERE presenca.viagemDia.id = :viagemDiaId
        AND responsavel.usuario.id = :usuarioId
    """)
    boolean existeVinculoResponsavel(@org.springframework.data.repository.query.Param("viagemDiaId") Long viagemDiaId,
                                     @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
