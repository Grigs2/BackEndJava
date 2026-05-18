package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    @Query("SELECT s FROM Solicitacao s WHERE s.dependente.id = :dependenteId AND s.viagem.id = :viagemId AND s.respondido = false")
    Optional<Solicitacao> findPendenteByDependenteAndViagem(Long dependenteId, Long viagemId);

    @Query("SELECT s FROM Solicitacao s WHERE s.responsavel.id = :responsavelId AND s.respondido = false")
    List<Solicitacao> findPendentesByResponsavelId(Long responsavelId);

    @Query("""
        SELECT s
        FROM Solicitacao s
        WHERE s.responsavel.id = :idResponsavel
        ORDER BY s.id DESC
    """)
    List<Solicitacao> findByResponsavelId(Long idResponsavel);
}
