package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.ImagemDocumentoMotorista;
import TioDaPerua.BackEndJava.enums.LadoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImagemDocumentoMotoristaRepository extends JpaRepository<ImagemDocumentoMotorista, Long> {
    List<ImagemDocumentoMotorista> findByDocumentoIdOrderByLadoAsc(Long documentoId);

    List<ImagemDocumentoMotorista> findByDocumentoIdIn(Collection<Long> documentoIds);

    Optional<ImagemDocumentoMotorista> findByDocumentoIdAndLado(Long documentoId, LadoDocumento lado);
}
