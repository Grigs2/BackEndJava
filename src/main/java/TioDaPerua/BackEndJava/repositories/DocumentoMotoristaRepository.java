package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.DocumentoMotorista;
import TioDaPerua.BackEndJava.enums.StatusDocumento;
import TioDaPerua.BackEndJava.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoMotoristaRepository extends JpaRepository<DocumentoMotorista, Long> {
    List<DocumentoMotorista> findByMotoristaIdOrderByIdDesc(Long motoristaId);

    Optional<DocumentoMotorista> findFirstByMotoristaIdAndTipoOrderByIdDesc(Long motoristaId, TipoDocumento tipo);

    List<DocumentoMotorista> findByStatusAndSubstituidoEmIsNullOrderBySubmetidoEmAsc(StatusDocumento status);

    List<DocumentoMotorista> findByVeiculoIdAndSubstituidoEmIsNull(Long veiculoId);

    boolean existsByMotoristaIdAndTipoAndStatus(Long motoristaId, TipoDocumento tipo, StatusDocumento status);
}
