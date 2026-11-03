package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByUsuarioId(Long usuarioId);
    List<Administrador> findAllByOrderByIdAsc();
}
