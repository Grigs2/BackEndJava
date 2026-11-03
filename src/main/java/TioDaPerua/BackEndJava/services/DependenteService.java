package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.repositories.DependenteRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.time.LocalDate;

@Service
public class DependenteService {

    @Autowired
    private DependenteRepository dependenteRepository;

    @Autowired
    private UsuarioService usuarioService;

    public Dependente alterarInfos(Dependente dependente, String senha, Long usuarioId) {
        if (dependente == null || dependente.getId() == null || senha == null || usuarioId == null) {
            throw new IllegalArgumentException("Dependente, ID, senha e usuário autenticado são obrigatórios");
        }
        if (dependente.getDataNascimento() != null
                && dependente.getDataNascimento().isAfter(LocalDate.now())) {
            throw new BusinessException("A data de nascimento do dependente não pode estar no futuro");
        }
        if (dependente.getCpf() != null && !dependente.getCpf().isBlank()) {
            String cpf = DocumentoFiscalValidator.normalizarCpf(dependente.getCpf());
            if (!DocumentoFiscalValidator.cpfValido(cpf)) {
                throw new BusinessException("CPF inválido");
            }
            if (dependenteRepository.existsByCpfAndIdNot(cpf, dependente.getId())) {
                throw new BusinessException("CPF já cadastrado");
            }
            dependente.setCpf(cpf);
        }
        
        Dependente existente = dependenteRepository.findByIdAndResponsavelUsuarioId(dependente.getId(), usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependente não encontrado para o responsável autenticado"));

        usuarioService.validarSenha(usuarioId, senha);

        existente.setNome(dependente.getNome());
        if (dependente.getCpf() != null && !dependente.getCpf().isBlank()) {
            existente.setCpf(dependente.getCpf());
        }
        existente.setDataNascimento(dependente.getDataNascimento());
        existente.setPeriodo(dependente.getPeriodo());
        existente.setEndereco(dependente.getEndereco());

        return dependenteRepository.save(existente);
    }
}
