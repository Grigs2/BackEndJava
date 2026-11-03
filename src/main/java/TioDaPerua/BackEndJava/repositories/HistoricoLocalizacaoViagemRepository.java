package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.HistoricoLocalizacaoViagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface HistoricoLocalizacaoViagemRepository extends JpaRepository<HistoricoLocalizacaoViagem, Long> {

    Page<HistoricoLocalizacaoViagem> findByViagemDia_IdAndRecebidoEmBetweenOrderByRecebidoEmAscIdAsc(
            Long viagemDiaId, Instant inicio, Instant fim, Pageable pageable);

    @Modifying
    @Query("DELETE FROM HistoricoLocalizacaoViagem historico WHERE historico.recebidoEm < :limite")
    int deleteExpirados(@Param("limite") Instant limite);
}
