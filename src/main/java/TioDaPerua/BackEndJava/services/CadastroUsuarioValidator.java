package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CadastroUsuarioValidator {

    private final UsuarioRepository usuarioRepository;

    public CadastroUsuarioValidator(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void validarNovoCadastro(Usuario usuario) {
        String email = normalizarEmail(usuario.getEmail());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("E-mail já cadastrado");
        }
        validarDadosObrigatorios(usuario, email);
        usuario.setEmail(email);
    }

    public void validarAtualizacao(Usuario usuario, Long id) {
        String email = normalizarEmail(usuario.getEmail());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessException("E-mail já cadastrado");
        }
        validarDadosObrigatorios(usuario, email);
        usuario.setEmail(email);
    }

    private void validarDadosObrigatorios(Usuario usuario, String email) {
        if (!DocumentoFiscalValidator.emailValido(email)) {
            throw new BusinessException("E-mail inválido");
        }
        String telefone = DocumentoFiscalValidator.normalizarTelefone(usuario.getTelefone());
        if (!DocumentoFiscalValidator.telefoneValido(telefone)) {
            throw new BusinessException("Telefone inválido; informe DDD e número");
        }
        if (usuario.getEndereco() == null || usuario.getEndereco().isBlank()) {
            throw new BusinessException("Endereço é obrigatório");
        }
        usuario.setTelefone(telefone);
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
