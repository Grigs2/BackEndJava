package TioDaPerua.BackEndJava.config;

import TioDaPerua.BackEndJava.entities.Administrador;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.AdministradorRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import TioDaPerua.BackEndJava.validation.SenhaPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.admin.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class AdministradorBootstrap implements ApplicationRunner {

    private final AdministradorRepository administradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdministradorBootstrap(AdministradorRepository administradorRepository,
                                  UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.admin.bootstrap.email:}") String email,
                                  @Value("${app.admin.bootstrap.password:}") String senha) {
        this.administradorRepository = administradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var perfis = administradorRepository.findAllByOrderByIdAsc();
        var usuariosAdmin = usuarioRepository.findAllByTipoPerfil("ADMIN");
        Set<Long> idsUsuariosAdmin = usuariosAdmin.stream().map(Usuario::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> idsPerfisAdmin = perfis.stream().map(perfil -> perfil.getUsuario().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (!idsPerfisAdmin.equals(idsUsuariosAdmin)) {
            throw new IllegalStateException("Bootstrap ADMIN encontrou usuário/perfil inconsistente; corrija os dados antes de iniciar");
        }
        for (Administrador perfil : perfis) {
            if (!"ADMIN".equals(perfil.getUsuario().getTipoPerfil())) {
                throw new IllegalStateException("Bootstrap ADMIN encontrou entidade ligada a usuário de outro perfil; corrija os dados antes de iniciar");
            }
        }
        if (!perfis.isEmpty()) {
            return;
        }
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalStateException("Configure ADMIN_EMAIL e ADMIN_PASSWORD para provisionar o primeiro administrador");
        }
        if (!SenhaPolicy.valida(senha)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD deve conter ao menos 12 caracteres e no máximo 72 bytes UTF-8");
        }
        String emailNormalizado = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalStateException("ADMIN_EMAIL já pertence a um usuário de outro perfil");
        }

        Usuario usuario = new Usuario(emailNormalizado, passwordEncoder.encode(senha), "", "", "ADMIN");
        Administrador administrador = new Administrador();
        administrador.setUsuario(usuario);
        administradorRepository.save(administrador);
    }
}
