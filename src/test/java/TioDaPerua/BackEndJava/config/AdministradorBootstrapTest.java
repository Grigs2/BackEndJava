package TioDaPerua.BackEndJava.config;

import TioDaPerua.BackEndJava.entities.Administrador;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.repositories.AdministradorRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradorBootstrapTest {

    @Mock private AdministradorRepository administradorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void provisionsFirstAdminWithHashedPasswordAndNoContactValues() throws Exception {
        when(administradorRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(usuarioRepository.findAllByTipoPerfil("ADMIN")).thenReturn(List.of());
        when(usuarioRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha segura para admin")).thenReturn("bcrypt-hash");

        AdministradorBootstrap bootstrap = new AdministradorBootstrap(
                administradorRepository, usuarioRepository, passwordEncoder,
                "admin@example.com", "Senha segura para admin");
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
        verify(administradorRepository).save(captor.capture());
        Usuario usuario = captor.getValue().getUsuario();
        assertEquals("admin@example.com", usuario.getEmail());
        assertEquals("bcrypt-hash", usuario.getSenha());
        assertEquals("", usuario.getEndereco());
        assertEquals("", usuario.getTelefone());
        assertEquals("ADMIN", usuario.getTipoPerfil());
    }

    @Test
    void refusesToBootstrapWithoutCredentialsWhenNoAdminExists() {
        when(administradorRepository.findAllByOrderByIdAsc()).thenReturn(List.of());
        when(usuarioRepository.findAllByTipoPerfil("ADMIN")).thenReturn(List.of());

        AdministradorBootstrap bootstrap = new AdministradorBootstrap(
                administradorRepository, usuarioRepository, passwordEncoder, "", "");

        assertThrows(IllegalStateException.class,
                () -> bootstrap.run(new DefaultApplicationArguments(new String[0])));
    }
}
