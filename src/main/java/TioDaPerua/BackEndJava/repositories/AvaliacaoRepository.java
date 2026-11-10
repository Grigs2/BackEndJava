package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByResponsavelIdAndMotoristaId(Long responsavelId, Long motoristaId);

    @Query("SELECT a FROM Avaliacao a JOIN FETCH a.responsavel WHERE a.motorista.id = :motoristaId ORDER BY a.criadoEm DESC")
    List<Avaliacao> listarDoMotorista(@Param("motoristaId") Long motoristaId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.motorista.id = :motoristaId")
    Double mediaDoMotorista(@Param("motoristaId") Long motoristaId);
}
