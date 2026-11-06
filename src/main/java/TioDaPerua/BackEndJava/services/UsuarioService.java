package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Administrador;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.AdministradorRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.exceptions.ResourceNotFoundException;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.validation.SenhaPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import TioDaPerua.BackEndJava.DTOs.UsuarioResumoDTO;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        } else if ("ADMIN".equals(u.getTipoPerfil())) {
            return u.getEmail();
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

    /** Novo aceite dos termos (versão vigente) pelo próprio usuário. */
    @Transactional
    public void registrarAceiteTermos(Long usuarioId, TermosService termosService) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        termosService.registrarAceite(usuario, true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario autenticar(String email, String senha) {
        if (email == null || email.isEmpty() || senha == null || senha.isEmpty()) {
            throw new IllegalArgumentException("Email e senha são obrigatórios");
        }
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (Boolean.TRUE.equals(usuario.getContaSuspensa())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        String senhaArmazenada = usuario.getSenha();
        if (senhaArmazenada != null && isBcryptHash(senhaArmazenada)
                && passwordEncoder.matches(senha, senhaArmazenada)) {
            return usuario;
        }

        if (senhaArmazenada == null || isBcryptHash(senhaArmazenada)
                || !MessageDigest.isEqual(
                        senha.getBytes(StandardCharsets.UTF_8),
                        senhaArmazenada.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        usuario.setSenha(passwordEncoder.encode(senha));
        return usuarioRepository.save(usuario);
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

    public Administrador logarAdministrador(Usuario usuario) {
        if (usuario == null) throw new IllegalArgumentException("Usuário não pode ser nulo");
        if (!"ADMIN".equals(usuario.getTipoPerfil())) {
            throw new BusinessException("Perfil do usuário não é Administrador");
        }
        return administradorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Administrador não encontrado para o usuário id: " + usuario.getId()));
    }

    public boolean verificarSenha(Long usuarioId, String senhaInformada) {
        if (usuarioId == null || senhaInformada == null) return false;
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return false;
        String senhaArmazenada = usuario.getSenha();
        if (senhaArmazenada == null) return false;
        if (isBcryptHash(senhaArmazenada)) {
            return passwordEncoder.matches(senhaInformada, senhaArmazenada);
        }
        if (!MessageDigest.isEqual(
                senhaInformada.getBytes(StandardCharsets.UTF_8),
                senhaArmazenada.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        usuario.setSenha(passwordEncoder.encode(senhaInformada));
        usuarioRepository.save(usuario);
        return true;
    }

    public void validarSenha(Long usuarioId, String senhaInformada) {
        if (!verificarSenha(usuarioId, senhaInformada)) {
            throw new UnauthorizedException("Senha inválida");
        }
    }

    @Transactional
    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        if (usuarioId == null || senhaAtual == null || senhaAtual.isBlank()) {
            throw new IllegalArgumentException("Usuário e senha atual são obrigatórios");
        }
        SenhaPolicy.exigirValida(novaSenha);
        validarSenha(usuarioId, senhaAtual);
        definirNovaSenha(usuarioId, novaSenha);
    }

    @Transactional
    public void definirNovaSenha(Long usuarioId, String novaSenha) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("ID do usuário é obrigatório");
        }
        SenhaPolicy.exigirValida(novaSenha);
        Usuario usuario = findByID(usuarioId);
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
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

    private boolean isBcryptHash(String value) {
        return value.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }
}
