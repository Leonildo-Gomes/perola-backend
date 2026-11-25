package cv.perola.modules.notifications.service;

import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import cv.perola.modules.notifications.domain.enums.EmailTemplate;

@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {
    private static final Locale PT_CV = Locale.of("pt", "CV");
    private final SpringTemplateEngine templateEngine;

    public String render(EmailTemplate template, Map<String, Object> variables) {
        Context context = new Context(PT_CV);
        context.setVariables(variables);
        return templateEngine.process(template.getTemplatePath(), context);
    }

}
