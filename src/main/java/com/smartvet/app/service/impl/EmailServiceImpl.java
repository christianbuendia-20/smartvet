package com.smartvet.app.service.impl;

import com.smartvet.app.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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

    @Override
    public void enviarConsultaPdf(String destinatario, String nombreCompleto,
                                   byte[] pdfBytes, Integer idConsulta) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Veterinaria Santa Victoria");
            helper.setTo(destinatario);
            helper.setSubject("Informe de Consulta #" + idConsulta + " — Veterinaria Santa Victoria");
            helper.setText(construirCuerpoHtmlPdf(nombreCompleto, idConsulta), true);
            helper.addAttachment("Consulta_" + idConsulta + ".pdf",
                    new ByteArrayResource(pdfBytes), "application/pdf");
            mailSender.send(mensaje);
            log.info("Informe PDF consulta #{} enviado a: {}", idConsulta, destinatario);
        } catch (Exception ex) {
            log.error("Fallo al enviar PDF de consulta #{} a {}", idConsulta, destinatario, ex);
            throw new RuntimeException("No se pudo enviar el informe. Verifique la configuración SMTP.", ex);
        }
    }

    @Override
    public void enviarRecuperacionContrasena(String destinatario, String nombreCompleto, String codigo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Veterinaria Santa Victoria");
            helper.setTo(destinatario);
            helper.setSubject("Restablecer tu contraseña - SmartVet");
            helper.setText(construirCuerpoHtmlRecuperacion(nombreCompleto, codigo), true);
            mailSender.send(mensaje);
            log.info("Correo de recuperación enviado a: {}", destinatario);
        } catch (Exception ex) {
            log.error("Fallo al enviar correo de recuperación a {}", destinatario, ex);
            throw new RuntimeException("No se pudo enviar el correo de recuperación. "
                    + "Verifica la configuración SMTP.", ex);
        }
    }

    private String construirCuerpoHtmlRecuperacion(String nombre, String codigo) {
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
                        <tr>
                          <td style="background:linear-gradient(135deg,#FF9800 0%%,#F57C00 100%%);
                                     padding:36px 40px;text-align:center;">
                            <p style="margin:0;font-size:13px;font-weight:700;color:rgba(255,255,255,0.85);
                                      text-transform:uppercase;letter-spacing:0.1em;">
                              Veterinaria Santa Victoria
                            </p>
                            <h1 style="margin:10px 0 0;font-size:26px;font-weight:900;color:#FFFFFF;
                                       letter-spacing:-0.02em;">
                              Restablecer contraseña
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px 40px 32px;">
                            <p style="margin:0 0 16px;font-size:16px;color:#3D3D3D;">
                              Hola, <strong>%s</strong> 👋
                            </p>
                            <p style="margin:0 0 28px;font-size:15px;color:#6B6B6B;line-height:1.7;">
                              Recibimos una solicitud para restablecer la contraseña de tu cuenta en SmartVet.
                              Usa el siguiente código de verificación para continuar:
                            </p>
                            <div style="background:#FFF3E0;border:2px dashed #FF9800;border-radius:12px;
                                        padding:28px 20px;text-align:center;margin-bottom:28px;">
                              <p style="margin:0 0 6px;font-size:12px;font-weight:700;
                                        color:#E65100;text-transform:uppercase;letter-spacing:0.12em;">
                                Tu código de recuperación
                              </p>
                              <p style="margin:0;font-size:48px;font-weight:900;color:#F57C00;
                                        letter-spacing:0.18em;line-height:1.1;">
                                %s
                              </p>
                            </div>
                            <p style="margin:0 0 24px;font-size:13px;color:#9E9E9E;line-height:1.65;">
                              Este código es válido para un solo uso. Si no solicitaste restablecer tu
                              contraseña, puedes ignorar este mensaje con seguridad.
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

    private String construirCuerpoHtmlPdf(String nombre, Integer idConsulta) {
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
                        <tr>
                          <td style="background:linear-gradient(135deg,#FF9800 0%%,#F57C00 100%%);
                                     padding:32px 40px;text-align:center;">
                            <p style="margin:0;font-size:12px;font-weight:700;color:rgba(255,255,255,0.85);
                                      text-transform:uppercase;letter-spacing:0.1em;">
                              Veterinaria Santa Victoria
                            </p>
                            <h1 style="margin:10px 0 0;font-size:22px;font-weight:900;color:#FFFFFF;">
                              Informe de Consulta Médica
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px 40px 28px;">
                            <p style="margin:0 0 14px;font-size:16px;color:#3D3D3D;">
                              Hola, <strong>%s</strong> 👋
                            </p>
                            <p style="margin:0 0 22px;font-size:15px;color:#6B6B6B;line-height:1.7;">
                              Adjunto encontrará el informe completo de la <strong>Consulta #%d</strong>
                              registrada en Veterinaria Santa Victoria.
                              El documento incluye los datos clínicos, signos vitales, diagnóstico,
                              tratamiento y receta médica cuando corresponda.
                            </p>
                            <div style="background:#FFF3E0;border-left:4px solid #FF9800;
                                        border-radius:8px;padding:16px 20px;margin-bottom:24px;">
                              <p style="margin:0;font-size:14px;color:#E65100;font-weight:600;">
                                📎 Archivo adjunto: Consulta_%d.pdf
                              </p>
                            </div>
                            <hr style="border:none;border-top:1px solid #E8E8E8;margin:0 0 22px;">
                            <p style="margin:0;font-size:12px;color:#9E9E9E;text-align:center;">
                              © 2025 Veterinaria Santa Victoria · Lima, Perú
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, idConsulta, idConsulta);
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
