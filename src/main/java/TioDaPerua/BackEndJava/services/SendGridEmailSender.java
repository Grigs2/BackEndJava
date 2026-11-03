package TioDaPerua.BackEndJava.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class SendGridEmailSender {
    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String passwordResetUrl;

    public SendGridEmailSender(@Value("${app.email.sendgrid.api-key:}") String apiKey,
                               @Value("${app.email.from:}") String fromEmail,
                               @Value("${app.email.password-reset-url:}") String passwordResetUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.sendgrid.com/v3")
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.passwordResetUrl = passwordResetUrl;
    }

    public void enviarRedefinicaoSenha(String destinatario, String token) {
        if (apiKey.isBlank() || fromEmail.isBlank() || passwordResetUrl.isBlank()) {
            throw new EmailDeliveryException(
                    "Configure SENDGRID_API_KEY, SENDGRID_FROM_EMAIL e PASSWORD_RESET_URL");
        }

        try {
            String link = UriComponentsBuilder.fromUriString(passwordResetUrl)
                    .queryParam("token", token)
                    .build()
                    .encode()
                    .toUriString();
            MailRequest request = new MailRequest(
                    List.of(new Personalization(List.of(new Address(destinatario)))),
                    new Address(fromEmail),
                    "Redefinição de senha - Tio da Perua",
                    List.of(new Content("text/plain",
                            "Use o link a seguir para redefinir sua senha. Ele expira em 15 minutos e só pode ser usado uma vez:\n\n"
                                    + link + "\n\nSe você não solicitou esta alteração, ignore este e-mail."))
            );
            restClient.post()
                    .uri("/mail/send")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | IllegalArgumentException exception) {
            String detalhe = exception instanceof RestClientResponseException responseException
                    ? "HTTP " + responseException.getStatusCode().value()
                    : exception.getClass().getSimpleName();
            throw new EmailDeliveryException("A API SendGrid recusou ou não recebeu o e-mail (" + detalhe + ")");
        }
    }

    private record Address(String email) {
    }

    private record Personalization(List<Address> to) {
    }

    private record Content(String type, String value) {
    }

    private record MailRequest(List<Personalization> personalizations,
                               Address from,
                               String subject,
                               List<Content> content) {
    }
}
