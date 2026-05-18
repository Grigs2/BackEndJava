package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Dependente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependenteRepository extends JpaRepository<Dependente, Long> {

    @Query("""
        SELECT d
        FROM Dependente d
        JOIN d.responsaveis r
        WHERE d.id = :dependenteId
        AND r.id = :responsavelId
    """)
    Optional<Dependente> findByIdAndResponsavelId(Long dependenteId, Long responsavelId);

    @Query("""
        SELECT d FROM Dependente d
        WHERE d.escola IS NOT NULL
        AND d.id NOT IN (
            SELECT vd.dependente.id FROM ViagemDependente vd WHERE vd.viagem.id = :viagemId AND vd.ativo = true
        )
        AND d.id NOT IN (
            SELECT s.dependente.id FROM Solicitacao s WHERE s.viagem.id = :viagemId AND s.respondido = false
        )
    """)
    List<Dependente> findDisponiveisParaViagem(Long viagemId);
}
