package cv.perola.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        public static final String SECURITY_SCHEME_NAME = "BearerAuth";

        @Bean
        public OpenAPI perolaOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Pérola SaaS API")
                                                .description("API RESTful da plataforma Pérola para descoberta, marcação e gestão de serviços de beleza e bem-estar em Cabo Verde.\n"
                                                                + "Garante integridade transacional contra overbooking e isolamento estrito multi-tenant.")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("Leonildo Gomes Developer")
                                                                .email("leogomescv@gmail.com")))
                                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                                .components(new Components()
                                                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                                                new SecurityScheme()
                                                                                .name(SECURITY_SCHEME_NAME)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")));
        }


        @Bean
        public OpenApiCustomizer apiVersionOpenApiCustomizer() {
                return openApi -> {
                        if (openApi.getPaths() == null) {
                                return;
                        }
                        Paths resolvedPaths = new Paths();
                        openApi.getPaths().forEach((path, pathItem) -> {
                                String resolvedPath = path.replace("{version}", "v1");
                                pathItem.readOperations().forEach(operation -> {
                                        if (operation.getParameters() != null) {
                                                operation.getParameters().removeIf(param -> "version".equalsIgnoreCase(param.getName()));
                                        }
                                });
                                resolvedPaths.addPathItem(resolvedPath, pathItem);
                        });
                        openApi.setPaths(resolvedPaths);
                };
        }
}
