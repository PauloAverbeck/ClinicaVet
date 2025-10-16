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

    @Value("${app.reset.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username:paverbeck@t3w.io}")
    private String from;

    public SmtpResetMailer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String toEmail, String token) {
        String link = baseUrl + "/reset?token=" + token;

        String subject = "Redefinição de senha - Clínica Vet";
        String html = """
                <html>
                  <body style="font-family:Arial, Helvetica, sans-serif; background-color:#f9f9f9; padding:40px;">
                    <table align="center" width="100%%" style="max-width:600px; background:white; border-radius:8px; box-shadow:0 4px 12px rgba(0,0,0,0.05); padding:24px;">
                      <tr>
                        <td style="text-align:center;">
                          <h2 style="color:#0077b6;">Redefinição de Senha</h2>
                          <p style="color:#333; font-size:15px;">
                            Recebemos uma solicitação para redefinir sua senha.<br>
                            Para continuar, clique no botão abaixo. O link expira em <strong>30 minutos</strong>.
                          </p>
                          <p style="margin:30px 0;">
                            <a href="%s" style="background-color:#0077b6; color:white; text-decoration:none; padding:12px 24px; border-radius:5px; display:inline-block;">
                              Redefinir minha senha
                            </a>
                          </p>
                          <p style="color:#555; font-size:13px;">
                            Se você não solicitou esta alteração, ignore este e-mail.<br>
                            Sua conta permanecerá segura.
                          </p>
                          <hr style="border:none; border-top:1px solid #eee; margin:30px 0;">
                          <p style="font-size:12px; color:#aaa;">
                            © 2025 Clínica Vet. Todos os direitos reservados.<br>
                            Este e-mail foi enviado automaticamente, por favor não responda.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(link);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao enviar e-mail de redefinição", e);
        }
    }
}
