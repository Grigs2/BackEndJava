package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.GeocodificacaoEndereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeocodificacaoEnderecoRepository extends JpaRepository<GeocodificacaoEndereco, Long> {
    Optional<GeocodificacaoEndereco> findByChave(String chave);
}
