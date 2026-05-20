package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DependenteService {

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private UsuarioService usuarioService;

    public Dependente alterarInfos(Dependente dependente, String senha) {
        if (dependente == null || dependente.getId() == null || senha == null) return null;
        
        Optional<Dependente> dependenteOpt = dependenteRepository.findById(dependente.getId());
        if (dependenteOpt.isEmpty()) return null;

        Dependente existente = dependenteOpt.get();
        
        // Validar senha contra o primeiro responsável
        if (existente.getResponsaveis() == null || existente.getResponsaveis().isEmpty()) return null;
        var responsavel = existente.getResponsaveis().get(0);
        if (responsavel.getUsuario() == null) return null;
        
        if (!usuarioService.verificarSenha(responsavel.getUsuario().getId(), senha)) {
            // Senha inválida - usaremos um marcador ou exceção se necessário, 
            // mas aqui vou retornar null e o controller trata.
            // Para diferenciar de "não encontrado", talvez seja melhor uma exceção.
            // Mas seguindo o padrão atual de retornar null... 
            // Vou lançar uma RuntimeException simples para o controller capturar 401.
            throw new RuntimeException("SENHA_INVALIDA");
        }

        existente.setNome(dependente.getNome());
        existente.setCpf(dependente.getCpf());
        existente.setDataNascimento(dependente.getDataNascimento());
        existente.setPeriodo(dependente.getPeriodo());
        existente.setEndereco(dependente.getEndereco());

        return dependenteRepository.save(existente);
    }
}
