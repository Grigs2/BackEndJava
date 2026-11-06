package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    @Query(value = "SELECT mot FROM Motorista mot WHERE mot.usuario.email = :email")
    public Optional<Motorista> findByEmail (String email);

    @Query(value = "SELECT mot FROM Motorista mot WHERE mot.usuario.id = :id")
    @EntityGraph(attributePaths = {"usuario", "usuario.enderecoOperacional"})
    public Optional<Motorista> findByUsuarioId (Long id);

    @Query("SELECT mot FROM Motorista mot JOIN FETCH mot.usuario usu "
            + "LEFT JOIN FETCH usu.enderecoOperacional WHERE mot.id = :id")
    Optional<Motorista> findByIdComEnderecoOperacional(Long id);

    Optional<Motorista> findByCpf(String cpf);

    /** Motoristas com conta ativa e veículo cadastrado, para a busca do responsável. */
    @Query("SELECT mot FROM Motorista mot JOIN FETCH mot.usuario usu "
            + "LEFT JOIN FETCH usu.enderecoOperacional JOIN FETCH mot.veiculo "
            + "WHERE usu.contaSuspensa IS NULL OR usu.contaSuspensa = false "
            + "ORDER BY mot.nome")
    java.util.List<Motorista> findAtivosComVeiculo();

    boolean existsByIdAndUsuario_Id(Long motoristaId, Long usuarioId);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);

    boolean existsByVeiculo_Placa(String placa);

    boolean existsByVeiculo_PlacaAndIdNot(String placa, Long motoristaId);
}
