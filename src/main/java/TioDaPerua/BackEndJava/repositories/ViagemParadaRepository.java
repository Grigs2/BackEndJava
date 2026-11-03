package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ViagemParada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ViagemParadaRepository extends JpaRepository<ViagemParada, Long> {

    @Query("""
        SELECT DISTINCT p FROM ViagemParada p
        LEFT JOIN FETCH p.participantes pp
        LEFT JOIN FETCH pp.presenca presenca
        WHERE p.viagemDia.id = :viagemDiaId
        ORDER BY p.ordem ASC
    """)
    List<ViagemParada> listarPorViagemDia(Long viagemDiaId);
}
