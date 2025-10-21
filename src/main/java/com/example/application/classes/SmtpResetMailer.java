package com.example.application.classes;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile({"prod", "smtp"})
public class SmtpResetMailer implements ResetMailer {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.product.name:Clínica Vet}")
    private String productName;

    public SmtpResetMailer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendProvisionalPassword(String toEmail, String provisionalPlain, String reason) {
        String subject = switch (reason) {
            case "signup" -> productName + " – Confirmação de cadastro";
            case "forgot" -> productName + " – Recuperação de senha";
            default -> productName + " – Acesso provisório";
        };

        String html = """
            <html>
              <body style="font-family:Arial, Helvetica, sans-serif; background-color:#f9f9f9; padding:40px;">
                <table align="center" width="100%%" style="max-width:600px; background:white; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.05); padding:24px;">
                  <tr>
                    <td style="text-align:center;">
                      <h2 style="color:#0077b6;">Sua senha provisória</h2>
                      <p style="color:#333; font-size:15px;">
                        %s
                      </p>
                      <p style="color:#333; font-size:14px; margin-top:25px;">
                        Use essa senha provisória para fazer login.<br>
                        Ao entrar, ela será automaticamente promovida a sua senha oficial.
                      </p>
                      <hr style="border:none; border-top:1px solid #eee; margin:30px 0;">
                      <p style="font-size:12px; color:#aaa;">
                        © 2025 %s. Todos os direitos reservados.<br>
                        Este e-mail foi enviado automaticamente, por favor não responda.
                      </p>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """.formatted("<strong>" + provisionalPlain + "</strong>", productName);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao enviar e-mail de senha provisória", e);
        }
    }
}
