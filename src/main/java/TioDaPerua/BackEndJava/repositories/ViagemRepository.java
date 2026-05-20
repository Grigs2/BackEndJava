package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemRepository extends JpaRepository<Viagem, Long> {

    @Query("SELECT v FROM Viagem v WHERE v.motorista.id = :motoristaId AND v.ativo = true")
    List<Viagem> findAtivasByMotoristaId(Long motoristaId);

    @Query("SELECT v FROM Viagem v WHERE v.motorista.id = :motoristaId AND v.ativo = false ORDER BY v.id DESC")
    List<Viagem> findHistoricoByMotoristaId(Long motoristaId);

    @Query("SELECT v FROM Viagem v WHERE v.motorista.id = :motoristaId AND v.periodo = :periodo AND v.ativo = true")
    Optional<Viagem> findByMotoristaIdAndPeriodo(Long motoristaId, Periodo periodo);
}
