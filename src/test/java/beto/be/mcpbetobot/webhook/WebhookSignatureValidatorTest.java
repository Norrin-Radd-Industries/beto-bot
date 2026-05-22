package beto.be.mcpbetobot.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureValidatorTest {

    private static final String SECRET = "my-super-secret-key-12345";
    private WebhookSignatureValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WebhookSignatureValidator(SECRET);
    }

    @Test
    void shouldValidateCorrectSignature() throws Exception {
        String payload = "{\"ref\": \"refs/heads/master\", \"repository\": {\"full_name\": \"test/repo\"}}";
        String expectedHmac = calculateHmac(payload, SECRET);
        String signatureHeader = "sha256=" + expectedHmac;

        assertTrue(validator.isValid(payload, signatureHeader));
    }

    @Test
    void shouldFailWhenSignatureIsIncorrect() {
        String payload = "{\"ref\": \"refs/heads/master\"}";
        String signatureHeader = "sha256=wrongsignaturevaluehere";

        assertFalse(validator.isValid(payload, signatureHeader));
    }

    @Test
    void shouldFailWhenSignatureHeaderDoesNotStartWithSha256() {
        String payload = "{\"ref\": \"refs/heads/master\"}";
        String signatureHeader = "invalidprefix=1234567890abcdef";

        assertFalse(validator.isValid(payload, signatureHeader));
    }

    @Test
    void shouldFailWhenSignatureHeaderIsNull() {
        String payload = "{\"ref\": \"refs/heads/master\"}";
        assertFalse(validator.isValid(payload, null));
    }

    @Test
    void shouldFailWhenExceptionOccursDuringValidation() {
        // A validator with a null secret will cause calculateHmac to throw an Exception
        WebhookSignatureValidator nullSecretValidator = new WebhookSignatureValidator(null);
        assertFalse(nullSecretValidator.isValid("payload", "sha256=123"));
    }

    private String calculateHmac(String data, String key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hmacBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
