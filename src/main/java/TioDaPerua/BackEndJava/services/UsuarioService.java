package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private EscolaRepository escolaRepository;

    public Usuario findByID(Long Id) {
        return usuarioRepository.findById(Id).orElse(null);
    }

    public Usuario findByEmail(String email) {
        if (email == null && email.isEmpty()) return null;
        return usuarioRepository.findByEmail(email).orElse(null);
    }

    public Usuario cadastrarUsuario(Usuario usuario) {
        if (usuario != null) return usuarioRepository.save(usuario);
        else return null;
    }

    public Usuario autenticar(String email, String senha) {
        if (email == null && email.isEmpty() && senha == null && senha.isEmpty()) return null;
        Usuario usuario = new Usuario(findByEmail(email));
        if (usuario == null) return null;

        if (senha.equals(usuario.getSenha())) {
            return usuario;
        } else return null;
    }

    public Motorista logarMotorista(Usuario usuario) {
        if (usuario == null) return null;
        if ("MOTORISTA".equals(usuario.getTipoPerfil())) {
            return motoristaRepository.findByUsuarioId(usuario.getId()).orElse(null);
        }
        return null;
    }

    public Responsavel logarResponsavel(Usuario usuario) {
        if (usuario == null) return null;
        if ("RESPONSAVEL".equals(usuario.getTipoPerfil())) {
            return responsavelRepository.findByUsuarioId(usuario.getId()).orElse(null);
        }
        return null;
    }

    public Escola logarEscola(Usuario usuario) {
        if (usuario == null) return null;
        if ("ESCOLA".equals(usuario.getTipoPerfil())) {
            return escolaRepository.findByUsuarioId(usuario.getId()).orElse(null);
        }
        return null;
    }

    public boolean verificarSenha(Long usuarioId, String senhaInformada) {
        if (usuarioId == null || senhaInformada == null) return false;
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return false;
        return senhaInformada.equals(usuario.getSenha());
    }

    public Usuario updateUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) return null;
        Usuario existente = usuarioRepository.findById(usuario.getId()).orElse(null);
        if (existente == null) return null;

        // Atualizar campos permitidos
        existente.setEmail(usuario.getEmail());
        existente.setEndereco(usuario.getEndereco());
        existente.setTelefone(usuario.getTelefone());
        
        // Se a senha vier no objeto e não for nula/vazia, pode atualizar também? 
        // A regra diz "Campo senha é obrigatório" e "Validar senha", mas não diz para trocar a senha.
        // Vou assumir que a senha NÃO deve ser alterada por este endpoint, apenas validada.
        
        return usuarioRepository.save(existente);
    }
}
