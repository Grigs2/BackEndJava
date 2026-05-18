package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Escola;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EscolaRepository extends JpaRepository<Escola, Long> {

    @Query("SELECT e FROM Escola e WHERE e.usuario.id = :id")
    Optional<Escola> findByUsuarioId(Long id);
}
