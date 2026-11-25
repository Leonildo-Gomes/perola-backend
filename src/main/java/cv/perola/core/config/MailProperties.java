package cv.perola.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "perola.mail")
public record MailProperties(
                boolean enabled,
                String fromAddress,
                String fromName,
                String frontendBaseUrl) {

}
