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
        SELECT DISTINCT vd
        FROM ViagemDependente vd
        JOIN FETCH vd.dependente d
        LEFT JOIN FETCH d.escola e
        WHERE vd.viagem.id = :viagemId AND vd.ativo = true
    """)
    List<ViagemDependente> findDetalhesByViagemId(Long viagemId);

    @Query("SELECT vd FROM ViagemDependente vd WHERE vd.dependente.id = :dependenteId AND vd.ativo = true")
    List<ViagemDependente> findAtivosByDependenteId(Long dependenteId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ViagemDependente vd SET vd.ativo = false WHERE vd.viagem.id = :viagemId")
    void desativarPorViagemId(Long viagemId);

    @Query("""
        SELECT DISTINCT vd
        FROM ViagemDependente vd
        JOIN FETCH vd.dependente d
        LEFT JOIN FETCH d.escola e
        LEFT JOIN FETCH e.usuario eu
        LEFT JOIN FETCH eu.enderecoOperacional
        LEFT JOIN FETCH d.responsaveis r
        LEFT JOIN FETCH r.usuario ru
        WHERE vd.viagem.id = :idViagem
        AND vd.ativo = true
    """)
    List<ViagemDependente> buscarDependentesCompletos(Long idViagem);

    @Query("""
        SELECT CASE WHEN COUNT(vd) > 0 THEN true ELSE false END
        FROM ViagemDependente vd
        JOIN vd.dependente dependente
        JOIN dependente.responsaveis responsavel
        WHERE vd.viagem.id = :viagemId
        AND responsavel.usuario.id = :usuarioId
    """)
    boolean existeVinculoResponsavel(@org.springframework.data.repository.query.Param("viagemId") Long viagemId,
                                     @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
