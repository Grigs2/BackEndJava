package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Escola;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscolaRepository extends JpaRepository<Escola, Long> {

    @Query("SELECT e FROM Escola e WHERE e.usuario.id = :id")
    @EntityGraph(attributePaths = {"usuario", "usuario.enderecoOperacional"})
    Optional<Escola> findByUsuarioId(Long id);

    @Query("SELECT e FROM Escola e JOIN FETCH e.usuario usu "
            + "LEFT JOIN FETCH usu.enderecoOperacional WHERE e.id = :id")
    Optional<Escola> findByIdComEnderecoOperacional(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Escola e WHERE e.id = :id")
    Optional<Escola> findByIdForUpdate(@Param("id") Long id);

    boolean existsByIdAndUsuario_Id(Long escolaId, Long usuarioId);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);

    Optional<Escola> findByCnpj(String cnpj);

    List<Escola> findTop20ByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    @EntityGraph(attributePaths = "usuario")
    @Query("""
        SELECT escola
        FROM Escola escola
        WHERE (:nome IS NULL OR LOWER(escola.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
        AND (:cnpj IS NULL OR escola.cnpj = :cnpj)
        AND (:suspensa IS NULL OR COALESCE(escola.usuario.contaSuspensa, false) = :suspensa)
        ORDER BY escola.id DESC
        """)
    Page<Escola> pesquisarAdministrativamente(
            @Param("nome") String nome,
            @Param("cnpj") String cnpj,
            @Param("suspensa") Boolean suspensa,
            Pageable pageable);
}
