package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {


    @Query(value = "SELECT res FROM Responsavel res WHERE res.usuario.id = :id")
    public Optional<Responsavel> findByUsuarioId (Long id);


}
