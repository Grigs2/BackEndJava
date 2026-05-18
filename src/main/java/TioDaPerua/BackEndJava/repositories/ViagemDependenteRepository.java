package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ViagemDependente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViagemDependenteRepository extends JpaRepository<ViagemDependente, Long> {

    @Query("SELECT vd FROM ViagemDependente vd WHERE vd.viagem.id = :viagemId AND vd.ativo = true")
    List<ViagemDependente> findAtivosByViagemId(Long viagemId);

    @Query("SELECT vd FROM ViagemDependente vd WHERE vd.viagem.id = :viagemId AND vd.dependente.id = :dependenteId")
    Optional<ViagemDependente> findByViagemIdAndDependenteId(Long viagemId, Long dependenteId);

    @Query("""
        SELECT vd
        FROM ViagemDependente vd
        JOIN FETCH vd.dependente d
        LEFT JOIN FETCH d.escola e
        WHERE vd.viagem.id = :viagemId AND vd.ativo = true
    """)
    List<ViagemDependente> findDetalhesByViagemId(Long viagemId);

    @Query("SELECT vd FROM ViagemDependente vd WHERE vd.dependente.id = :dependenteId AND vd.ativo = true")
    List<ViagemDependente> findAtivosByDependenteId(Long dependenteId);
    @Query("""
        SELECT vd
        FROM ViagemDependente vd
        JOIN FETCH vd.dependente d
        LEFT JOIN FETCH d.escola e
        LEFT JOIN FETCH e.usuario eu
        LEFT JOIN FETCH d.responsaveis r
        LEFT JOIN FETCH r.usuario ru
        WHERE vd.viagem.id = :idViagem
        AND vd.ativo = true
    """)
    List<ViagemDependente> buscarDependentesCompletos(Long idViagem);
}
