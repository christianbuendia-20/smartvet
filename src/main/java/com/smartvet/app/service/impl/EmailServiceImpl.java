package com.smartvet.app.service.impl;

import com.smartvet.app.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCodigoVerificacion(String destinatario, String nombreCompleto, String codigo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Veterinaria Santa Victoria");
            helper.setTo(destinatario);
            helper.setSubject("Verificación de Cuenta - SmartVet");
            helper.setText(construirCuerpoHtml(nombreCompleto, codigo), true);
            mailSender.send(mensaje);
            log.info("Correo de verificación enviado a: {}", destinatario);
        } catch (Exception ex) {
            log.error("Fallo al enviar correo de verificación a {}", destinatario, ex);
            throw new RuntimeException("No se pudo enviar el correo de verificación. "
                    + "Verifica la configuración SMTP.", ex);
        }
    }

    private String construirCuerpoHtml(String nombre, String codigo) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#F7F7F7;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F7F7F7;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#FFFFFF;border-radius:16px;overflow:hidden;
                                    box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                        <!-- Cabecera naranja -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#FF9800 0%%,#F57C00 100%%);
                                     padding:36px 40px;text-align:center;">
                            <p style="margin:0;font-size:13px;font-weight:700;color:rgba(255,255,255,0.85);
                                      text-transform:uppercase;letter-spacing:0.1em;">
                              Veterinaria Santa Victoria
                            </p>
                            <h1 style="margin:10px 0 0;font-size:26px;font-weight:900;color:#FFFFFF;
                                       letter-spacing:-0.02em;">
                              Verifica tu cuenta
                            </h1>
                          </td>
                        </tr>

                        <!-- Cuerpo -->
                        <tr>
                          <td style="padding:40px 40px 32px;">
                            <p style="margin:0 0 16px;font-size:16px;color:#3D3D3D;">
                              Hola, <strong>%s</strong> 👋
                            </p>
                            <p style="margin:0 0 28px;font-size:15px;color:#6B6B6B;line-height:1.7;">
                              Gracias por registrarte en SmartVet. Para activar tu cuenta ingresa el
                              siguiente código de verificación:
                            </p>

                            <!-- Código OTP -->
                            <div style="background:#FFF3E0;border:2px dashed #FF9800;border-radius:12px;
                                        padding:28px 20px;text-align:center;margin-bottom:28px;">
                              <p style="margin:0 0 6px;font-size:12px;font-weight:700;
                                        color:#E65100;text-transform:uppercase;letter-spacing:0.12em;">
                                Tu código de verificación
                              </p>
                              <p style="margin:0;font-size:48px;font-weight:900;color:#F57C00;
                                        letter-spacing:0.18em;line-height:1.1;">
                                %s
                              </p>
                            </div>

                            <p style="margin:0 0 24px;font-size:13px;color:#9E9E9E;line-height:1.65;">
                              Este código es válido para una sola verificación. Si no creaste esta
                              cuenta, ignora este mensaje.
                            </p>

                            <hr style="border:none;border-top:1px solid #E8E8E8;margin:0 0 24px;">

                            <p style="margin:0;font-size:13px;color:#9E9E9E;text-align:center;">
                              © 2025 Veterinaria Santa Victoria · Lima, Perú
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, codigo);
    }
}
