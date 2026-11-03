package TioDaPerua.BackEndJava.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class PasswordResetFingerprint {
    private final byte[] keyBytes;

    public PasswordResetFingerprint(@Value("${app.security.jwt.secret-base64}") String encodedSecret) {
        this.keyBytes = Base64.getDecoder().decode(encodedSecret);
    }

    public String hash(String purpose, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] fingerprint = mac.doFinal((purpose + ":" + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(fingerprint);
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 não está disponível nesta JVM", exception);
        }
    }
}
