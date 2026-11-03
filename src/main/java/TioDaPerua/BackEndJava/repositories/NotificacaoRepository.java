package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    @Query("SELECT n FROM Notificacao n WHERE n.destinatario.id = :usuarioId ORDER BY n.data DESC")
    List<Notificacao> findByDestinatarioId(Long usuarioId);

    @Query("SELECT n FROM Notificacao n WHERE n.destinatario.id = :usuarioId AND n.visto = false ORDER BY n.data DESC")
    List<Notificacao> findNaoVistasByDestinatarioId(Long usuarioId);

    boolean existsByIdAndDestinatario_Id(Long notificacaoId, Long usuarioId);

    int deleteByDataBefore(LocalDateTime limite);
}
