package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.HistoricoViagemDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HistoricoViagemDiaRepository extends JpaRepository<HistoricoViagemDia, Long> {

    @Query("""
        SELECT h FROM HistoricoViagemDia h
        JOIN FETCH h.ator
        WHERE h.viagemDia.id = :viagemDiaId
        ORDER BY h.ocorridoEm ASC, h.id ASC
    """)
    List<HistoricoViagemDia> listarPorViagemDia(Long viagemDiaId);
}
