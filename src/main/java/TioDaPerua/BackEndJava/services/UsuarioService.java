package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import TioDaPerua.BackEndJava.DTOs.UsuarioResumoDTO;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<UsuarioResumoDTO> listarUsuariosResumo() {
        return usuarioRepository.findAll().stream()
                .map(this::getUsuarioResumo)
                .collect(Collectors.toList());
    }

    public UsuarioResumoDTO getUsuarioResumo(Usuario u) {
        if (u == null) return null;
        return new UsuarioResumoDTO(
                u.getId(),
                getNomeByUsuario(u),
                u.getTipoPerfil(),
                u.getTelefone()
        );
    }

    public String getNomeByUsuario(Usuario u) {
        if (u == null) return "Sistema";
        if ("MOTORISTA".equals(u.getTipoPerfil())) {
            return motoristaRepository.findByUsuarioId(u.getId()).map(Motorista::getNome).orElse("Desconhecido");
        } else if ("RESPONSAVEL".equals(u.getTipoPerfil())) {
            return responsavelRepository.findByUsuarioId(u.getId()).map(Responsavel::getNome).orElse("Desconhecido");
        } else if ("ESCOLA".equals(u.getTipoPerfil())) {
            return escolaRepository.findByUsuarioId(u.getId()).map(Escola::getNome).orElse("Desconhecido");
        }
        return "Desconhecido";
    }

    public Usuario findByID(Long Id) {
        return usuarioRepository.findById(Id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + Id));
    }

    public Usuario findByEmail(String email) {
        if (email == null || email.isEmpty()) throw new IllegalArgumentException("Email não pode ser vazio");
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
    }

    public Usuario cadastrarUsuario(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Usuário não pode ser nulo");
        return usuarioRepository.save(usuario);
    }

    public Usuario autenticar(String email, String senha) {
        if (email == null || email.isEmpty() || senha == null || senha.isEmpty()) {
            throw new IllegalArgumentException("Email e senha são obrigatórios");
        }
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (senha.equals(usuario.getSenha())) {
            return usuario;
        } else {
            throw new UnauthorizedException("Credenciais inválidas");
        }
    }

    public Motorista logarMotorista(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Usuário não pode ser nulo");
        if (!"MOTORISTA".equals(usuario.getTipoPerfil())) {
            throw new BusinessException("Perfil do usuário não é Motorista");
        }
        return motoristaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Motorista não encontrado para o usuário id: " + usuario.getId()));
    }

    public Responsavel logarResponsavel(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Usuário não pode ser nulo");
        if (!"RESPONSAVEL".equals(usuario.getTipoPerfil())) {
            throw new BusinessException("Perfil do usuário não é Responsável");
        }
        return responsavelRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado para o usuário id: " + usuario.getId()));
    }

    public Escola logarEscola(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Usuário não pode ser nulo");
        if (!"ESCOLA".equals(usuario.getTipoPerfil())) {
            throw new BusinessException("Perfil do usuário não é Escola");
        }
        return escolaRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada para o usuário id: " + usuario.getId()));
    }

    public boolean verificarSenha(Long usuarioId, String senhaInformada) {
        if (usuarioId == null || senhaInformada == null) return false;
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return false;
        return senhaInformada.equals(usuario.getSenha());
    }

    public void validarSenha(Long usuarioId, String senhaInformada) {
        if (!verificarSenha(usuarioId, senhaInformada)) {
            throw new UnauthorizedException("Senha inválida");
        }
    }

    public Usuario updateUsuario(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) throw new IllegalArgumentException("ID do usuário é obrigatório");
        Usuario existente = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + usuario.getId()));

        existente.setEmail(usuario.getEmail());
        existente.setEndereco(usuario.getEndereco());
        existente.setTelefone(usuario.getTelefone());
        
        return usuarioRepository.save(existente);
    }
}
