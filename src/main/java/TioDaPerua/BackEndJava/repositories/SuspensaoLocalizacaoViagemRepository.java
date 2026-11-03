package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.SuspensaoLocalizacaoViagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuspensaoLocalizacaoViagemRepository extends JpaRepository<SuspensaoLocalizacaoViagem, Long> {

    Optional<SuspensaoLocalizacaoViagem> findByViagemDia_Id(Long viagemDiaId);

    boolean existsByViagemDia_Id(Long viagemDiaId);
}
