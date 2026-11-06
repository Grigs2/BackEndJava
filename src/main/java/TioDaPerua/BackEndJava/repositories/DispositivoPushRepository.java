package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.DispositivoPush;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {

    Optional<DispositivoPush> findByExpoToken(String expoToken);

    @Query("SELECT d FROM DispositivoPush d JOIN FETCH d.usuario u WHERE u.id IN :usuarioIds AND d.ativo = true")
    List<DispositivoPush> findAtivosDosUsuarios(Collection<Long> usuarioIds);
}
