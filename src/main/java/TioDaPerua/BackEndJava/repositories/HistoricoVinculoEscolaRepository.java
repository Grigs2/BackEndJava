package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.HistoricoVinculoEscola;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoVinculoEscolaRepository extends JpaRepository<HistoricoVinculoEscola, Long> {
    @EntityGraph(attributePaths = "ator")
    List<HistoricoVinculoEscola> findByVinculo_IdOrderByOcorridoEmAscIdAsc(Long vinculoId);
}
