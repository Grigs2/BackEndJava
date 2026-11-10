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

    /** Solicitações ainda sem resposta das viagens do motorista, das mais novas para as mais antigas. */
    @Query("""
        SELECT s FROM Solicitacao s
        WHERE s.viagem.motorista.id = :motoristaId
        AND s.respondido = false
        ORDER BY s.id DESC
    """)
    List<Solicitacao> findAbertasByMotoristaId(Long motoristaId);

    @Query("""
        SELECT DISTINCT s
        FROM Solicitacao s
        JOIN s.dependente.responsaveis responsavel
        WHERE responsavel.id = :responsavelId
        AND s.respondido = false
        """)
    List<Solicitacao> findPendentesByResponsavelId(Long responsavelId);

    @Query("""
        SELECT DISTINCT s
        FROM Solicitacao s
        JOIN s.dependente.responsaveis responsavel
        WHERE responsavel.id = :idResponsavel
        AND s.dataFim IS NULL
        ORDER BY s.id DESC
    """)
    List<Solicitacao> findByResponsavelId(Long idResponsavel);

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM Solicitacao s
        JOIN s.dependente.responsaveis responsavel
        WHERE s.id = :solicitacaoId
        AND responsavel.usuario.id = :usuarioId
    """)
    boolean pertenceAoUsuario(@org.springframework.data.repository.query.Param("solicitacaoId") Long solicitacaoId,
                              @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Solicitacao s SET s.dataFim = :dataFim WHERE s.viagem.id = :viagemId AND s.dataFim IS NULL")
    void encerrarPorViagemId(Long viagemId, java.time.LocalDateTime dataFim);
}
