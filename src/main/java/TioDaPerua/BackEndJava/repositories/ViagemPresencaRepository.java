package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemPresencaRepository extends JpaRepository<ViagemPresenca, Long> {

    @Query("""
        SELECT vp FROM ViagemPresenca vp
        JOIN FETCH vp.dependente
        WHERE vp.viagemDia.id = :viagemDiaId
    """)
    List<ViagemPresenca> findByViagemDiaId(Long viagemDiaId);

    @Query("""
        SELECT vp FROM ViagemPresenca vp 
        WHERE vp.dependente.id = :dependenteId 
        ORDER BY 
            vp.viagemDia.data DESC, 
            CASE vp.viagemDia.status 
                WHEN 'EM_ANDAMENTO' THEN 1 
                WHEN 'PLANEJADA' THEN 2 
                WHEN 'FINALIZADA' THEN 3 
                ELSE 4 
            END ASC, 
            vp.id DESC
    """)
    List<ViagemPresenca> findHistoricoByDependenteId(Long dependenteId);

    @Query("SELECT vp FROM ViagemPresenca vp WHERE vp.viagemDia.id = :viagemDiaId AND vp.dependente.id = :dependenteId")
    Optional<ViagemPresenca> findByViagemDiaIdAndDependenteId(Long viagemDiaId, Long dependenteId);
}
