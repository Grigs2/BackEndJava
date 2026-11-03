package TioDaPerua.BackEndJava.services;

import TioDaPerua.BackEndJava.entities.PasswordResetToken;
import TioDaPerua.BackEndJava.exceptions.UnauthorizedException;
import TioDaPerua.BackEndJava.repositories.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PasswordResetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final AutenticacaoService autenticacaoService;
    private final PasswordResetRequestTransaction requestTransaction;
    private final PasswordResetEmailDispatcher emailDispatcher;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                AutenticacaoService autenticacaoService,
                                PasswordResetRequestTransaction requestTransaction,
                                PasswordResetEmailDispatcher emailDispatcher) {
        this.tokenRepository = tokenRepository;
        this.autenticacaoService = autenticacaoService;
        this.requestTransaction = requestTransaction;
        this.emailDispatcher = emailDispatcher;
    }

    public void solicitarRedefinicao(String email, String ip) {
        requestTransaction.preparar(email, ip).ifPresent(emailRequest -> {
            try {
                emailDispatcher.enviar(emailRequest);
            } catch (TaskRejectedException exception) {
                LOGGER.error("A fila de envio de e-mail de redefinição está cheia; a solicitação poderá ser refeita.",
                        exception);
            }
        });
    }

    @Transactional
    public void redefinirSenha(String tokenBruto, String novaSenha) {
        if (tokenBruto == null || tokenBruto.isBlank()) {
            throw new UnauthorizedException("Token de redefinição inválido ou expirado");
        }
        Instant agora = Instant.now();
        PasswordResetToken token = tokenRepository.buscarValidoParaAtualizacao(
                        TokenHasher.sha256Hex(tokenBruto), agora)
                .orElseThrow(() -> new UnauthorizedException(
                        "Token de redefinição inválido ou expirado"));

        Long usuarioId = token.getUsuario().getId();
        tokenRepository.invalidarTokensAtivosDoUsuario(usuarioId, agora);
        autenticacaoService.definirNovaSenha(usuarioId, novaSenha);
    }

}
