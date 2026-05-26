package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.Veiculo;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MotoristaService {

    @Autowired
    private MotoristaRepository motoristaRepository;

    public Motorista cadastraMotorista(Motorista motorista) {
        if (motorista == null) throw new IllegalArgumentException("Motorista não pode ser nulo");
        return motoristaRepository.save(motorista);
    }

    public Motorista cadastrarVeiculo(long id_motorista, Veiculo veiculo) {
        if (veiculo == null) throw new IllegalArgumentException("Veículo não pode ser nulo");
        Motorista motoristaExistente = motoristaRepository.findById(id_motorista)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + id_motorista));

        motoristaExistente.setVeiculo(veiculo);
        return motoristaRepository.save(motoristaExistente);
    }

    public Motorista alterarInfos(Motorista motorista) {
        if (motorista == null || motorista.getId() == null) throw new IllegalArgumentException("ID do motorista é obrigatório");
        
        Motorista existente = motoristaRepository.findById(motorista.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado com id: " + motorista.getId()));
        
        // Atualizar campos editáveis do motorista
        existente.setNome(motorista.getNome());
        existente.setDataNascimento(motorista.getDataNascimento());
        existente.setCpf(motorista.getCpf());
        existente.setCnh(motorista.getCnh());
        
        if (motorista.getUsuario() != null && existente.getUsuario() != null) {
            Usuario uExistente = existente.getUsuario();
            Usuario uNovo = motorista.getUsuario();
            uExistente.setEmail(uNovo.getEmail());
            uExistente.setEndereco(uNovo.getEndereco());
            uExistente.setTelefone(uNovo.getTelefone());
        }

        return motoristaRepository.save(existente);
    }
}
