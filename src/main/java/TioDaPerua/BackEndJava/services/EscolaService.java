package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.EnderecoOperacional;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.validation.SenhaPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
public class EscolaService {

    @Autowired
    private EscolaRepository escolaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TermosService termosService;

    @Autowired
    private CadastroUsuarioValidator cadastroUsuarioValidator;

    @Autowired
    private EnderecoOperacionalService enderecoOperacionalService;

    public Escola cadastrarEscola(Escola escola) {
        if (escola == null) throw new IllegalArgumentException("Escola não pode ser nula");
        if (escola.getNome() == null || escola.getNome().isBlank()) {
            throw new BusinessException("Nome da escola é obrigatório");
        }
        if (escola.getId() != null || escola.getUsuario() == null || escola.getUsuario().getId() != null
                || escola.getUsuario().getSenha() == null || escola.getUsuario().getSenha().isBlank()) {
            throw new IllegalArgumentException("Cadastro deve conter credenciais novas e válidas");
        }
        escola.setCnpj(validarCnpj(escola.getCnpj()));
        cadastroUsuarioValidator.validarNovoCadastro(escola.getUsuario());
        SenhaPolicy.exigirValida(escola.getUsuario().getSenha());
        termosService.registrarAceite(escola.getUsuario(),
                escola.getUsuario().getAceitouTermos());
        escola.getUsuario().setSenha(passwordEncoder.encode(escola.getUsuario().getSenha()));
        if (escola.getUsuario().getEnderecoOperacional() == null) {
            throw new BusinessException("Endereço operacional estruturado é obrigatório no cadastro");
        }
        escola.getUsuario().setEnderecoOperacional(
                enderecoOperacionalService.validar(escola.getUsuario().getEnderecoOperacional()));
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
        EnderecoOperacional enderecoAtualizado = escola.getUsuario() == null
                ? null : escola.getUsuario().getEnderecoOperacional();
        if (enderecoAtualizado != null) {
            enderecoAtualizado = enderecoOperacionalService.validar(enderecoAtualizado);
        }
        
        Escola existente = escolaRepository.findByIdComEnderecoOperacional(escola.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada com id: " + escola.getId()));

        existente.setNome(escola.getNome());
        existente.setAdmResponsavel(escola.getAdmResponsavel());
        if (escola.getCnpj() != null) {
            String cnpj = DocumentoFiscalValidator.normalizarCnpj(escola.getCnpj());
            if (!DocumentoFiscalValidator.cnpjValido(cnpj)) {
                throw new BusinessException("CNPJ inválido");
            }
            if (escolaRepository.existsByCnpjAndIdNot(cnpj, existente.getId())) {
                throw new BusinessException("CNPJ já cadastrado");
            }
            existente.setCnpj(cnpj);
        }

        if (escola.getUsuario() != null && existente.getUsuario() != null) {
            Usuario uExistente = existente.getUsuario();
            Usuario uNovo = escola.getUsuario();
            cadastroUsuarioValidator.validarAtualizacao(uNovo, uExistente.getId());
            uExistente.setEmail(uNovo.getEmail());
            uExistente.setEndereco(uNovo.getEndereco());
            uExistente.setTelefone(uNovo.getTelefone());
            if (enderecoAtualizado != null) {
                enderecoOperacionalService.atualizarUsuario(uExistente, enderecoAtualizado);
            }
        }

        return escolaRepository.save(existente);
    }

    private String validarCnpj(String cnpjInformado) {
        String cnpj = DocumentoFiscalValidator.normalizarCnpj(cnpjInformado);
        if (!DocumentoFiscalValidator.cnpjValido(cnpj)) {
            throw new BusinessException("CNPJ inválido");
        }
        if (escolaRepository.existsByCnpj(cnpj)) {
            throw new BusinessException("CNPJ já cadastrado");
        }
        return cnpj;
    }
}
