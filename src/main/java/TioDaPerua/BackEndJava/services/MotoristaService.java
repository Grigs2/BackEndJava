package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.util.Optional;

@Service
public class MotoristaService {

    @Autowired
    private MotoristaRepository motoristaRepository;

    public Motorista cadastraMotorista(Motorista motorista) {
        if (motorista == null) return null;
        return motoristaRepository.save(motorista);
    }

    public Motorista cadastrarVeiculo(long id_motorista, Veiculo veiculo) {
        if (veiculo == null ) return null;
        Optional<Motorista> motorista = motoristaRepository.findById(id_motorista);

        if (motorista.isPresent()) {
            Motorista motoristaExistente = motorista.get();
            motoristaExistente.setVeiculo(veiculo);
            return motoristaRepository.save(motoristaExistente);
        }
        return null;

    }
}
