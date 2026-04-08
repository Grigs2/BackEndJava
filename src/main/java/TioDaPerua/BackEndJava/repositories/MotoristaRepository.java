package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    @Query(value = "SELECT mot FROM Motorista mot WHERE mot.usuario.email = :email")
    public Optional<Motorista> findByEmail (String email);

    @Query(value = "SELECT mot FROM Motorista mot WHERE mot.usuario.id = :id")
    public Optional<Motorista> findByUsuarioId (Long id);
}
