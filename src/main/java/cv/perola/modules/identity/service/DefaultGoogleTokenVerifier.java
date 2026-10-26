package cv.perola.modules.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cv.perola.core.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGoogleTokenVerifier implements GoogleTokenVerifier {

    private final ObjectMapper objectMapper;

    @Override
    public GoogleUserData verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new InvalidTokenException("Token Google não pode ser vazio.");
        }

        // Suporte para tokens de teste / mock (ex: mock-sub:email@perola.cv:Nome:https://avatar)
        if (idToken.startsWith("mock-") || idToken.startsWith("test-")) {
            return parseMockToken(idToken);
        }

        try {
            // Em conformidade com OIDC, um ID Token é um JWT com 3 partes: header.payload.signature
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new InvalidTokenException("Formato de token Google OIDC inválido.");
            }

            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(payloadJson);

            String sub = root.path("sub").asText(null);
            String email = root.path("email").asText(null);
            String name = root.path("name").asText(null);
            String picture = root.path("picture").asText(null);

            if (sub == null || email == null) {
                throw new InvalidTokenException("Token Google não contém as reivindicações mínimas obrigatórias ('sub', 'email').");
            }

            return new GoogleUserData(sub, email, name != null ? name : email, picture);
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Erro ao decodificar token Google OIDC: {}", ex.getMessage());
            throw new InvalidTokenException("Falha na validação do token Google: " + ex.getMessage());
        }
    }

    private GoogleUserData parseMockToken(String token) {
        String[] parts = token.split(":", 4);
        String sub = parts[0];
        String email = parts.length > 1 ? parts[1] : sub + "@perola.cv";
        String name = parts.length > 2 ? parts[2] : "Utilizador Google";
        String picture = parts.length > 3 ? parts[3] : "https://lh3.googleusercontent.com/avatar.jpg";
        return new GoogleUserData(sub, email, name, picture);
    }
}
