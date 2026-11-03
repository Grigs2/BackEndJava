package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.exceptions.BusinessException;
import TioDaPerua.BackEndJava.repositories.EscolaRepository;
import TioDaPerua.BackEndJava.repositories.MotoristaRepository;
import TioDaPerua.BackEndJava.repositories.ResponsavelRepository;
import TioDaPerua.BackEndJava.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MotoristaRepository motoristaRepository;
    @Mock
    private ResponsavelRepository responsavelRepository;
    @Mock
    private EscolaRepository escolaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void migratesLegacyPasswordToHashAfterSuccessfulAuthentication() {
        Usuario usuario = usuarioComSenha("senha-legada");
        when(usuarioRepository.findByEmail("responsavel@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("senha-legada")).thenReturn("$2b$12$hash");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        assertSame(usuario, usuarioService.autenticar("responsavel@example.com", "senha-legada"));

        verify(usuarioRepository).save(usuario);
        org.junit.jupiter.api.Assertions.assertEquals("$2b$12$hash", usuario.getSenha());
    }

    @Test
    void rejectsIncorrectLegacyPasswordWithoutChangingIt() {
        Usuario usuario = usuarioComSenha("senha-legada");
        when(usuarioRepository.findByEmail("responsavel@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(UnauthorizedException.class,
                () -> usuarioService.autenticar("responsavel@example.com", "incorreta"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verifiesHashedPasswordWithoutReencodingIt() {
        String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        Usuario usuario = usuarioComSenha(hash);
        when(usuarioRepository.findByEmail("responsavel@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", hash)).thenReturn(true);

        assertSame(usuario, usuarioService.autenticar("responsavel@example.com", "senha-segura"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void changesPasswordOnlyAfterVerifyingCurrentPassword() {
        String oldHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        Usuario usuario = usuarioComSenha(oldHash);
        when(usuarioRepository.findById(41L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha atual", oldHash)).thenReturn(true);
        when(passwordEncoder.encode("Nova senha forte 123")).thenReturn("novo-hash");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.alterarSenha(41L, "senha atual", "Nova senha forte 123");

        org.junit.jupiter.api.Assertions.assertEquals("novo-hash", usuario.getSenha());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordDoesNotMatch() {
        String oldHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        Usuario usuario = usuarioComSenha(oldHash);
        when(usuarioRepository.findById(41L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha incorreta", oldHash)).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> usuarioService.alterarSenha(41L, "senha incorreta", "Nova senha forte 123"));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejectsNewPasswordOutsideTheApprovedPolicy() {
        assertThrows(BusinessException.class,
                () -> usuarioService.alterarSenha(41L, "senha atual", "curta"));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    private Usuario usuarioComSenha(String senha) {
        Usuario usuario = new Usuario();
        usuario.setId(41L);
        usuario.setEmail("responsavel@example.com");
        usuario.setSenha(senha);
        usuario.setTipoPerfil("RESPONSAVEL");
        return usuario;
    }
}
