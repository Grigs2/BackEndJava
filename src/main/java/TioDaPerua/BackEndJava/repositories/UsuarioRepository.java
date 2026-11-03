package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query (value = "SELECT usu FROM Usuario usu WHERE usu.email = :email")
    public Optional<Usuario> findByEmail (String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findAllByTipoPerfil(String tipoPerfil);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT usuario FROM Usuario usuario WHERE usuario.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT usuario FROM Usuario usuario WHERE usuario.tipoPerfil = 'ADMIN' ORDER BY usuario.id")
    List<Usuario> bloquearAdministradores();

    @Query("""
        SELECT usuario
        FROM Usuario usuario
        WHERE (:id IS NULL OR usuario.id = :id)
        AND (:tipoPerfil IS NULL OR usuario.tipoPerfil = :tipoPerfil)
        AND (:email IS NULL OR LOWER(usuario.email) = LOWER(CAST(:email AS string)))
        AND (:suspensa IS NULL OR COALESCE(usuario.contaSuspensa, false) = :suspensa)
        ORDER BY usuario.id DESC
        """)
    Page<Usuario> pesquisarAdministrativamente(
            @Param("id") Long id,
            @Param("tipoPerfil") String tipoPerfil,
            @Param("email") String email,
            @Param("suspensa") Boolean suspensa,
            Pageable pageable);

    @Query("SELECT COUNT(usuario) FROM Usuario usuario "
            + "WHERE usuario.tipoPerfil = 'ADMIN' AND COALESCE(usuario.contaSuspensa, false) = false")
    long contarAdministradoresAtivos();
}
