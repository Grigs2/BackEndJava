package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.AuditoriaAdministrativa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditoriaAdministrativaRepository
        extends JpaRepository<AuditoriaAdministrativa, Long> {

    @EntityGraph(attributePaths = "ator")
    @Query("""
        SELECT evento
        FROM AuditoriaAdministrativa evento
        WHERE (:atorId IS NULL OR evento.ator.id = :atorId)
        AND (:acao IS NULL OR evento.acao = :acao)
        AND (:tipoAlvo IS NULL OR evento.tipoAlvo = :tipoAlvo)
        AND (:alvoId IS NULL OR evento.alvoId = :alvoId)
        AND evento.ocorridoEm >= COALESCE(:inicio, evento.ocorridoEm)
        AND evento.ocorridoEm <= COALESCE(:fim, evento.ocorridoEm)
        """)
    Page<AuditoriaAdministrativa> pesquisar(
            @Param("atorId") Long atorId,
            @Param("acao") String acao,
            @Param("tipoAlvo") String tipoAlvo,
            @Param("alvoId") Long alvoId,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            Pageable pageable);

    int deleteByOcorridoEmBefore(Instant limite);
}
