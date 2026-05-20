package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
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

    public Motorista alterarInfos(Motorista motorista) {
        if (motorista == null || motorista.getId() == null) return null;
        Optional<Motorista> motoristaOpt = motoristaRepository.findById(motorista.getId());
        if (motoristaOpt.isEmpty()) return null;

        Motorista existente = motoristaOpt.get();
        
        // Atualizar campos editáveis do motorista
        existente.setNome(motorista.getNome());
        existente.setDataNascimento(motorista.getDataNascimento());
        existente.setCpf(motorista.getCpf());
        existente.setCnh(motorista.getCnh());
        
        // Atualizar campos do usuário via UsuarioService se necessário, 
        // mas aqui vamos assumir que o motorista já vem com o Usuario preenchido 
        // e vamos atualizar os campos permitidos do Usuario existente.
        if (motorista.getUsuario() != null && existente.getUsuario() != null) {
            Usuario uExistente = existente.getUsuario();
            Usuario uNovo = motorista.getUsuario();
            uExistente.setEmail(uNovo.getEmail());
            uExistente.setEndereco(uNovo.getEndereco());
            uExistente.setTelefone(uNovo.getTelefone());
            // Senha não é alterada aqui.
        }

        return motoristaRepository.save(existente);
    }
}
