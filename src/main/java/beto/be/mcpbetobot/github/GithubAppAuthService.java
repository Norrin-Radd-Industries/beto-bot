package beto.be.mcpbetobot.github;

import io.jsonwebtoken.Jwts;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class GithubAppAuthService {

    @Value("${GITHUB_APP_ID}")
    private String appId;

    @Value("${GITHUB_APP_KEY}")
    private Resource key;

    @Value("${GITHUB_APP_INSTALL_ID}")
    private String installationId;

    public String getInstallationToken() {
        try {
            String privateKey = StreamUtils.copyToString(
                    key.getInputStream(), StandardCharsets.UTF_8);
            GitHub gitHubApp = new GitHubBuilder()
                    .withJwtToken(generateJwt(appId, privateKey))
                    .build();

            GHAppInstallation installation = gitHubApp
                    .getApp()
                    .getInstallationById(Long.parseLong(installationId));
            return installation.createToken().create().getToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GH app token", e);
        }
    }

    private String generateJwt(String appId, String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // get the contents for the key
        String keyContent = key
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // decode the key and transform into java privateKey object
        byte[] encodedKey = Base64.getDecoder().decode(keyContent);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));

        Date now = new Date(System.currentTimeMillis());
        Date expire = new Date (System.currentTimeMillis() + 600000);

        return Jwts.builder()
                .issuedAt(now)
                .expiration(expire)
                .issuer(appId)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

}
