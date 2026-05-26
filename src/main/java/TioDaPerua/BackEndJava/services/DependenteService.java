package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
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
        if (dependente == null || dependente.getId() == null || senha == null) {
            throw new IllegalArgumentException("Dependente, ID e senha são obrigatórios");
        }
        
        Dependente existente = dependenteRepository.findById(dependente.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado com id: " + dependente.getId()));
        
        // Validar senha contra o primeiro responsável
        if (existente.getResponsaveis() == null || existente.getResponsaveis().isEmpty()) {
            throw new BusinessException("Dependente não possui responsáveis vinculados");
        }
        var responsavel = existente.getResponsaveis().get(0);
        if (responsavel.getUsuario() == null) {
            throw new BusinessException("Responsável não possui usuário vinculado");
        }
        
        usuarioService.validarSenha(responsavel.getUsuario().getId(), senha);

        existente.setNome(dependente.getNome());
        existente.setCpf(dependente.getCpf());
        existente.setDataNascimento(dependente.getDataNascimento());
        existente.setPeriodo(dependente.getPeriodo());
        existente.setEndereco(dependente.getEndereco());

        return dependenteRepository.save(existente);
    }
}
