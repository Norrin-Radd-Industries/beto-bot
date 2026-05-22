package beto.be.mcpbetobot.data.github;

import io.jsonwebtoken.Jwts;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Date;

@ConfigurationProperties(prefix = "github")
public record GithubAppAuthService(String clientId, Resource key, String installationId) {

    public String getInstallationToken() {
        try {
            String privateKey = StreamUtils.copyToString(
                    key.getInputStream(), StandardCharsets.UTF_8);
            GitHub gitHubApp = new GitHubBuilder()
                    .withJwtToken(generateJwt(clientId, privateKey))
                    .build();

            GHAppInstallation installation = gitHubApp
                    .getApp()
                    .getInstallationById(Long.parseLong(installationId));
            return installation.createToken().create().getToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GH app token", e);
        }
    }

    private String generateJwt(String clientId, String key) {
        PrivateKey privateKey;

        try (PEMParser pemParser = new PEMParser(new StringReader(key))) {
            Object object = pemParser.readObject();

            PEMKeyPair pemKeyPair = (PEMKeyPair) object;

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            privateKey = converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Date now = new Date(System.currentTimeMillis());
        Date expire = new Date (System.currentTimeMillis() + 60000);

        return Jwts.builder()
                .issuedAt(now)
                .expiration(expire)
                .issuer(clientId)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

}
