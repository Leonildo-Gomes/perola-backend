package cv.perola.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://perola.cv/errors/unauthorized");
        problem.put("title", "Não Autorizado");
        problem.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        problem.put("detail", "Autenticação necessária para aceder a este recurso.");
        problem.put("instance", request.getRequestURI());
        problem.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
