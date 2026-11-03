package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.HistoricoOrdemParada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface HistoricoOrdemParadaRepository extends JpaRepository<HistoricoOrdemParada, Long> {

    @EntityGraph(attributePaths = {"ator", "parada"})
    List<HistoricoOrdemParada> findByViagemDia_IdOrderByOcorridoEmAscIdAsc(Long viagemDiaId);
}
