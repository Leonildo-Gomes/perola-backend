package cv.perola.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DotenvEnvironmentPostProcessorTest {

    @Test
    @DisplayName("Deve carregar propriedades do arquivo .env para o Environment do Spring")
    void shouldLoadDotenvPropertiesIntoEnvironment() {
        // Arrange
        DotenvEnvironmentPostProcessor processor = new DotenvEnvironmentPostProcessor();
        ConfigurableEnvironment environment = new StandardEnvironment();
        SpringApplication application = new SpringApplication();

        // Act
        processor.postProcessEnvironment(environment, application);

        // Assert
        // Verifica se a propriedade do .env foi disponibilizada no Environment
        String jwtSecret = environment.getProperty("JWT_SECRET");
        assertThat(jwtSecret)
                .isNotNull()
                .isEqualTo("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
    }
}

