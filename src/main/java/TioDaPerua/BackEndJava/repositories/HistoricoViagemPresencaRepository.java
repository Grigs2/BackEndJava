package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.HistoricoViagemPresenca;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoricoViagemPresencaRepository extends JpaRepository<HistoricoViagemPresenca, Long> {

    @EntityGraph(attributePaths = {"presenca", "presenca.dependente", "ator"})
    List<HistoricoViagemPresenca> findByPresenca_ViagemDia_IdOrderByOcorridoEmAscIdAsc(Long viagemDiaId);

    @EntityGraph(attributePaths = {"presenca", "presenca.dependente", "presenca.viagemDia"})
    Optional<HistoricoViagemPresenca> findByAtor_IdAndEventoClienteId(Long atorUsuarioId, UUID eventoClienteId);
}
