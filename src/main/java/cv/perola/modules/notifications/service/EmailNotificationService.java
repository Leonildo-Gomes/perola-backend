package cv.perola.modules.notifications.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import cv.perola.core.config.MailProperties;
import cv.perola.modules.notifications.dto.EmailMessageRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final MailProperties mailProperties;

    @Override
    public void send(EmailMessageRequest request) {
        String htmlBody = templateRenderer.render(request.template(), request.variables());
        if (!mailProperties.enabled()) {
            logger.info(
                    "[MAIL-DRY-RUN] E-mail renderizado com sucesso (envio SMTP desativado) | Para: {} | Assunto: {} | Template: {}",
                    request.to(),
                    request.subject(),
                    request.template().name());
            return;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.fromAddress(), mailProperties.fromName());
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(mimeMessage);
            logger.info("E-mail enviado com sucesso para {} [template={}]", request.to(), request.template().name());
        } catch (MessagingException | UnsupportedEncodingException | MailException ex) {
            // Não relança exceção para não quebrar o fluxo principal já commitado; regista
            // erro estruturado
            logger.error(
                    "Falha ao enviar e-mail para {} [template={}]: {}",
                    request.to(),
                    request.template().name(),
                    ex.getMessage(),
                    ex);
        }
    }

}
