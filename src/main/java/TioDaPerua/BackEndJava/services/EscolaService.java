package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EscolaService {

    @Autowired
    private EscolaRepository escolaRepository;

    public Escola cadastrarEscola(Escola escola) {
        if (escola == null) return null;
        return escolaRepository.save(escola);
    }

    public List<Escola> listarEscolas() {
        return escolaRepository.findAll();
    }

    public Escola buscarPorId(Long id) {
        return escolaRepository.findById(id).orElse(null);
    }

    public Escola alterarInfos(Escola escola) {
        if (escola == null || escola.getId() == null) return null;
        Optional<Escola> escolaOpt = escolaRepository.findById(escola.getId());
        if (escolaOpt.isEmpty()) return null;

        Escola existente = escolaOpt.get();
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
