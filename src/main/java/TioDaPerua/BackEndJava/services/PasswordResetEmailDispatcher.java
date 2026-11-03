package TioDaPerua.BackEndJava.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetEmailDispatcher.class);

    private final SendGridEmailSender emailSender;

    public PasswordResetEmailDispatcher(SendGridEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async("passwordResetEmailExecutor")
    public void enviar(PasswordResetEmailRequest request) {
        try {
            emailSender.enviarRedefinicaoSenha(request.email(), request.token());
        } catch (EmailDeliveryException exception) {
            LOGGER.error("Falha no envio de e-mail de redefinição de senha. Verifique a configuração do provedor.",
                    exception);
        }
    }
}
