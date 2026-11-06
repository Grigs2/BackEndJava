package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.AusenciaInformada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AusenciaInformadaRepository extends JpaRepository<AusenciaInformada, Long> {

    List<AusenciaInformada> findByDependenteIdAndDataGreaterThanEqualOrderByDataAsc(Long dependenteId, LocalDate aPartirDe);

    List<AusenciaInformada> findByDependenteIdInAndData(Collection<Long> dependenteIds, LocalDate data);

    boolean existsByDependenteIdAndDataAndTrecho(Long dependenteId, LocalDate data, String trecho);

    boolean existsByDependenteIdAndData(Long dependenteId, LocalDate data);

    Optional<AusenciaInformada> findByIdAndDependenteId(Long id, Long dependenteId);
}
