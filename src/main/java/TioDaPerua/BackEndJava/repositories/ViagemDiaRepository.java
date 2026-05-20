package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ViagemDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemDiaRepository extends JpaRepository<ViagemDia, Long> {

    @Query("SELECT vd FROM ViagemDia vd WHERE vd.viagem.id = :viagemId AND vd.data = :data")
    Optional<ViagemDia> findByViagemIdAndData(Long viagemId, LocalDate data);

    @Query("SELECT vd FROM ViagemDia vd WHERE vd.viagem.motorista.id = :motoristaId ORDER BY vd.id DESC")
    List<ViagemDia> findByMotoristaId(Long motoristaId);
}
