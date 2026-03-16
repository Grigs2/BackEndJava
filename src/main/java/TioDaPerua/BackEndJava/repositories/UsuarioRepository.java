package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query (value = "SELECT usu FROM Usuario usu WHERE usu.email = :email")
    public Optional<Usuario> findByEmail (String email);

}
