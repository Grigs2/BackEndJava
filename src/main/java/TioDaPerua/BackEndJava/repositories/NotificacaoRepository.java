package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    @Query("SELECT n FROM Notificacao n WHERE n.destinatario.id = :usuarioId ORDER BY n.data DESC")
    List<Notificacao> findByDestinatarioId(Long usuarioId);

    @Query("SELECT n FROM Notificacao n WHERE n.destinatario.id = :usuarioId AND n.visto = false ORDER BY n.data DESC")
    List<Notificacao> findNaoVistasByDestinatarioId(Long usuarioId);

    /** Central de notificações: todas ou só as não lidas, mais recentes primeiro, paginado. */
    @Query("""
        SELECT n FROM Notificacao n
        WHERE n.destinatario.id = :usuarioId
        AND (:somenteNaoLidas = false OR n.visto = false OR n.visto IS NULL)
        ORDER BY n.data DESC, n.id DESC
    """)
    List<Notificacao> buscarDoUsuario(@Param("usuarioId") Long usuarioId,
                                      @Param("somenteNaoLidas") boolean somenteNaoLidas,
                                      Pageable pageable);

    /** Igual a {@link #buscarDoUsuario}, filtrando pelos tipos de um grupo (registros sem tipo contam como GERAL). */
    @Query("""
        SELECT n FROM Notificacao n
        WHERE n.destinatario.id = :usuarioId
        AND (:somenteNaoLidas = false OR n.visto = false OR n.visto IS NULL)
        AND COALESCE(n.tipo, 'GERAL') IN :tipos
        ORDER BY n.data DESC, n.id DESC
    """)
    List<Notificacao> buscarDoUsuarioPorTipos(@Param("usuarioId") Long usuarioId,
                                              @Param("somenteNaoLidas") boolean somenteNaoLidas,
                                              @Param("tipos") Collection<String> tipos,
                                              Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.destinatario.id = :usuarioId AND (n.visto = false OR n.visto IS NULL)")
    long contarNaoLidas(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("""
        UPDATE Notificacao n SET n.visto = true, n.lidaEm = :agora
        WHERE n.destinatario.id = :usuarioId AND (n.visto = false OR n.visto IS NULL)
    """)
    int marcarTodasLidas(@Param("usuarioId") Long usuarioId, @Param("agora") LocalDateTime agora);

    boolean existsByChaveDedup(String chaveDedup);

    /** Usado para limitar a frequência dos avisos de atraso de uma viagem. */
    boolean existsByTipoAndViagemDiaIdAndDataAfter(String tipo, Long viagemDiaId, LocalDateTime depoisDe);

    boolean existsByIdAndDestinatario_Id(Long notificacaoId, Long usuarioId);

    int deleteByDataBefore(LocalDateTime limite);
}
