package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ConsentimentoLocalizacao;
import TioDaPerua.BackEndJava.enums.TipoEscopoConsentimentoLocalizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface ConsentimentoLocalizacaoRepository extends JpaRepository<ConsentimentoLocalizacao, Long> {

    Optional<ConsentimentoLocalizacao> findTopByUsuario_IdAndEscopoAndDependenteIsNullAndVersaoPoliticaOrderByIdDesc(
            Long usuarioId, TipoEscopoConsentimentoLocalizacao escopo, String versaoPolitica);

    Optional<ConsentimentoLocalizacao> findTopByUsuario_IdAndEscopoAndDependente_IdAndVersaoPoliticaOrderByIdDesc(
            Long usuarioId,
            TipoEscopoConsentimentoLocalizacao escopo,
            Long dependenteId,
            String versaoPolitica);

    @Query("""
        SELECT consentimento
        FROM ConsentimentoLocalizacao consentimento
        WHERE consentimento.escopo = :escopo
        AND consentimento.dependente.id IN :dependenteIds
        AND consentimento.usuario.id IN :usuarioIds
        AND consentimento.versaoPolitica = :versaoPolitica
        AND consentimento.id = (
            SELECT MAX(maisRecente.id)
            FROM ConsentimentoLocalizacao maisRecente
            WHERE maisRecente.escopo = consentimento.escopo
            AND maisRecente.dependente.id = consentimento.dependente.id
            AND maisRecente.usuario.id = consentimento.usuario.id
            AND maisRecente.versaoPolitica = consentimento.versaoPolitica
        )
    """)
    List<ConsentimentoLocalizacao> findLatestByDependentesAndUsuarios(
            @Param("escopo") TipoEscopoConsentimentoLocalizacao escopo,
            @Param("dependenteIds") Collection<Long> dependenteIds,
            @Param("usuarioIds") Collection<Long> usuarioIds,
            @Param("versaoPolitica") String versaoPolitica);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM ConsentimentoLocalizacao antigo
        WHERE antigo.ocorridoEm < :limite
        AND EXISTS (
            SELECT maisRecente.id
            FROM ConsentimentoLocalizacao maisRecente
            WHERE maisRecente.usuario.id = antigo.usuario.id
            AND maisRecente.escopo = antigo.escopo
            AND maisRecente.versaoPolitica = antigo.versaoPolitica
            AND (
                maisRecente.dependente.id = antigo.dependente.id
                OR (maisRecente.dependente IS NULL AND antigo.dependente IS NULL)
            )
            AND maisRecente.id > antigo.id
        )
    """)
    int deleteAntigosPreservandoUltimo(@Param("limite") Instant limite);
}
