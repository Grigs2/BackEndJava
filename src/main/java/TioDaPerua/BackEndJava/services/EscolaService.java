package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EscolaService {

    @Autowired
    private EscolaRepository escolaRepository;

    public Escola cadastrarEscola(Escola escola) {
        if (escola == null) throw new IllegalArgumentException("Escola não pode ser nula");
        return escolaRepository.save(escola);
    }

    public List<Escola> listarEscolas() {
        return escolaRepository.findAll();
    }

    public Escola buscarPorId(Long id) {
        return escolaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada com id: " + id));
    }

    public Escola alterarInfos(Escola escola) {
        if (escola == null || escola.getId() == null) throw new IllegalArgumentException("ID da escola é obrigatório");
        
        Escola existente = escolaRepository.findById(escola.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada com id: " + escola.getId()));

        existente.setNome(escola.getNome());
        existente.setAdmResponsavel(escola.getAdmResponsavel());

        if (escola.getUsuario() != null && existente.getUsuario() != null) {
            Usuario uExistente = existente.getUsuario();
            Usuario uNovo = escola.getUsuario();
            uExistente.setEmail(uNovo.getEmail());
            uExistente.setEndereco(uNovo.getEndereco());
            uExistente.setTelefone(uNovo.getTelefone());
        }

        return escolaRepository.save(existente);
    }
}
