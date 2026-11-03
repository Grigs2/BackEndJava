package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.LocalizacaoViagemAtual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface LocalizacaoViagemAtualRepository extends JpaRepository<LocalizacaoViagemAtual, Long> {

    Optional<LocalizacaoViagemAtual> findByViagemDia_Id(Long viagemDiaId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LocalizacaoViagemAtual atual WHERE atual.viagemDia.id = :viagemDiaId")
    int deleteByViagemDiaId(@Param("viagemDiaId") Long viagemDiaId);
}
