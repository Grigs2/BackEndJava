package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.PasswordResetToken;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.repositories.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private AutenticacaoService autenticacaoService;
    @Mock private PasswordResetRequestTransaction requestTransaction;
    @Mock private PasswordResetEmailDispatcher emailDispatcher;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokenRepository, autenticacaoService,
                requestTransaction, emailDispatcher);
    }

    @Test
    void dispatchesPreparedEmailWithoutExposingTheAccountInTheControllerContract() {
        PasswordResetEmailRequest emailRequest =
                new PasswordResetEmailRequest("responsavel@example.com", "raw-one-time-token");
        when(requestTransaction.preparar("responsavel@example.com", "192.0.2.10"))
                .thenReturn(Optional.of(emailRequest));

        service.solicitarRedefinicao("responsavel@example.com", "192.0.2.10");

        verify(emailDispatcher).enviar(emailRequest);
    }

    @Test
    void invalidatesTheResetTokenAndRevokesSessionsAfterPasswordReset() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuario(usuario());
        when(tokenRepository.buscarValidoParaAtualizacao(anyString(), any()))
                .thenReturn(Optional.of(token));

        service.redefinirSenha("one-time-token", "Senha nova segura 123!");

        verify(tokenRepository).invalidarTokensAtivosDoUsuario(eq(51L), any());
        verify(autenticacaoService).definirNovaSenha(51L, "Senha nova segura 123!");
    }

    @Test
    void rejectsExpiredOrAlreadyUsedResetTokens() {
        when(tokenRepository.buscarValidoParaAtualizacao(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.redefinirSenha("invalid-token", "Senha nova segura 123!"));

        verify(autenticacaoService, never()).definirNovaSenha(any(), anyString());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(51L);
        usuario.setEmail("responsavel@example.com");
        return usuario;
    }
}
