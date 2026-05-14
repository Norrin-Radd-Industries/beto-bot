package beto.be.mcpbetobot.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class WebhookSignatureValidator {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookSecret;

    public WebhookSignatureValidator(@Value("${github.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            logger.warn("Missing or invalid signature header");
            return false;
        }

        try {
            String expectedSignature = calculateHmac(payload);
            String actualSignature = signature.substring(7); // Remove "sha256="
            return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                                         actualSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }

    private String calculateHmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        Mac mac = Mac.getInstance(HMAC_SHA256);
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
