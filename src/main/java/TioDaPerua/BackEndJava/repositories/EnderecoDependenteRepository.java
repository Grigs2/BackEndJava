package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.EnderecoDependente;
import TioDaPerua.BackEndJava.enums.FinalidadeEndereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnderecoDependenteRepository extends JpaRepository<EnderecoDependente, Long> {

    List<EnderecoDependente> findByDependenteIdAndAtivoTrueOrderById(Long dependenteId);

    List<EnderecoDependente> findByDependenteIdInAndAtivoTrue(List<Long> dependenteIds);

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM EnderecoDependente e
        WHERE e.dependente.id = :dependenteId
        AND e.ativo = true
        AND e.id <> :enderecoId
        AND (
            e.finalidade = :finalidade
            OR e.finalidade = :ambos
            OR :finalidade = :ambos
        )
        """)
    boolean existeConflitoDeFinalidade(
            @Param("dependenteId") Long dependenteId,
            @Param("enderecoId") Long enderecoId,
            @Param("finalidade") FinalidadeEndereco finalidade,
            @Param("ambos") FinalidadeEndereco ambos);

    Optional<EnderecoDependente> findByIdAndDependenteId(Long id, Long dependenteId);
}
