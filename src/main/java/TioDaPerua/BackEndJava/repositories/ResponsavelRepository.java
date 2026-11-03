package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.enums.StatusConviteResponsavel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {


    @Query(value = "SELECT res FROM Responsavel res WHERE res.usuario.id = :id")
    public Optional<Responsavel> findByUsuarioId (Long id);

    boolean existsByIdAndUsuario_Id(Long responsavelId, Long usuarioId);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    @Query("""
        SELECT dependente.id AS dependenteId, responsavel.usuario.id AS usuarioId
        FROM Dependente dependente
        JOIN dependente.responsaveis responsavel
        WHERE dependente.id IN :dependenteIds
    """)
    List<DependenteResponsavelUsuarioProjection> findUsuariosAtivosPorDependentes(
            @Param("dependenteIds") Collection<Long> dependenteIds);

    @Query("""
        SELECT dependente.id AS dependenteId,
               responsavel.id AS responsavelId,
               convite.categoria AS categoria,
               CASE WHEN dependente.responsavelCriador.id = responsavel.id THEN true ELSE false END
                   AS responsavelCriador,
               convite.respondidoEm AS vinculadoEm
        FROM Dependente dependente
        JOIN dependente.responsaveis responsavel
        LEFT JOIN ConviteResponsavel convite
            ON convite.dependente.id = dependente.id
            AND convite.convidado.id = responsavel.id
            AND convite.status = :statusAceito
        WHERE (:dependenteId IS NULL OR dependente.id = :dependenteId)
        AND (:responsavelId IS NULL OR responsavel.id = :responsavelId)
        ORDER BY dependente.id DESC, responsavel.id DESC
        """)
    Page<AdminVinculoResponsavelProjection> pesquisarVinculosAdministrativamente(
            @Param("dependenteId") Long dependenteId,
            @Param("responsavelId") Long responsavelId,
            @Param("statusAceito") StatusConviteResponsavel statusAceito,
            Pageable pageable);

}
