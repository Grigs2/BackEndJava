package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
