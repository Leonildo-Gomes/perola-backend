package cv.perola.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://perola.cv/errors/access-denied");
        problem.put("title", "Acesso Proibido");
        problem.put("status", HttpServletResponse.SC_FORBIDDEN);
        problem.put("detail", "Não possui permissões para aceder a este recurso.");
        problem.put("instance", request.getRequestURI());
        problem.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
