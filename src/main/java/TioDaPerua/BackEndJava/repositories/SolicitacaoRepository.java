package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.enums.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    @Query("SELECT s FROM Solicitacao s WHERE s.dependente.id = :dependenteId AND s.viagem.id = :viagemId AND s.respondido = false")
    Optional<Solicitacao> findPendenteByDependenteAndViagem(Long dependenteId, Long viagemId);

    /** Solicitações sem resposta do aluno, em qualquer viagem. */
    @Query("SELECT s FROM Solicitacao s WHERE s.dependente.id = :dependenteId AND s.respondido = false")
    List<Solicitacao> findPendentesByDependenteId(Long dependenteId);

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

    /** Marca como expiradas as solicitações sem resposta cujo prazo já passou. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Solicitacao s
        SET s.respondido = true, s.aceito = false, s.status = :expirada
        WHERE s.respondido = false
        AND s.dataExpiracao IS NOT NULL
        AND s.dataExpiracao < :agora
    """)
    int expirarVencidas(@org.springframework.data.repository.query.Param("agora") LocalDateTime agora,
                        @org.springframework.data.repository.query.Param("expirada") StatusSolicitacao expirada);
}
