package com.smartvet.app.service.impl;

import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.repository.CitaRepository;
import com.smartvet.app.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final DateTimeFormatter FMT_EMAIL =
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy 'a las' HH:mm 'h'",
                    Locale.forLanguageTag("es"));

    private final JavaMailSender  mailSender;
    private final CitaRepository  citaRepository;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailServiceImpl(JavaMailSender mailSender, CitaRepository citaRepository) {
        this.mailSender      = mailSender;
        this.citaRepository  = citaRepository;
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
    public void enviarComprobanteCita(String destinatario, String nombreCompleto,
                                       byte[] pdfBytes, Integer idCita) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Veterinaria Santa Victoria");
            helper.setTo(destinatario);
            helper.setSubject("Comprobante de Cita #" + idCita + " — Veterinaria Santa Victoria");
            helper.setText(construirCuerpoHtmlCita(nombreCompleto, idCita), true);
            helper.addAttachment(
                    "Comprobante_Cita_" + idCita + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf");
            mailSender.send(mensaje);
            log.info("Comprobante de cita #{} enviado a: {}", idCita, destinatario);
        } catch (Exception ex) {
            log.error("Fallo al enviar comprobante de cita #{} a {}", idCita, destinatario, ex);
            throw new RuntimeException("No se pudo enviar el comprobante. Verifique la configuración SMTP.", ex);
        }
    }

    @Async
    @Override
    public void enviarCancelacionCita(Integer idCita) {
        Cita cita = citaRepository.findByIdWithDetalle(idCita).orElse(null);
        if (cita == null) {
            log.warn("enviarCancelacionCita: cita_id={} no encontrada, correo omitido", idCita);
            return;
        }

        Mascota mascota  = cita.getMascota();
        Usuario prop     = mascota.getPropietario();
        String nombreVet = cita.getVeterinario().getUsuario().getNombres()
                         + " " + cita.getVeterinario().getUsuario().getApellidos();

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Veterinaria Santa Victoria");
            helper.setTo(prop.getEmail());
            helper.setSubject("Confirmación de cancelación de cita — Veterinaria Santa Victoria");
            helper.setText(construirCuerpoHtmlCancelacion(
                    prop.getNombres(),
                    mascota.getNombre(),
                    cita.getFechaHora().format(FMT_EMAIL),
                    cita.getTipoCita().name(),
                    nombreVet,
                    idCita), true);
            mailSender.send(mensaje);
            log.info("Correo de cancelación enviado: cita_id={} → {}", idCita, prop.getEmail());
        } catch (Exception ex) {
            log.error("Fallo al enviar correo de cancelación: cita_id={} → {}",
                    idCita, prop.getEmail(), ex);
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

    private String construirCuerpoHtmlCita(String nombre, Integer idCita) {
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
                                     padding:32px 40px;text-align:center;">
                            <p style="margin:0;font-size:12px;font-weight:700;color:rgba(255,255,255,0.85);
                                      text-transform:uppercase;letter-spacing:0.1em;">
                              Veterinaria Santa Victoria
                            </p>
                            <h1 style="margin:10px 0 0;font-size:22px;font-weight:900;color:#FFFFFF;">
                              ¡Cita Confirmada!
                            </h1>
                          </td>
                        </tr>

                        <!-- Cuerpo -->
                        <tr>
                          <td style="padding:36px 40px 28px;">
                            <p style="margin:0 0 14px;font-size:16px;color:#3D3D3D;">
                              Hola, <strong>%s</strong> 👋
                            </p>
                            <p style="margin:0 0 22px;font-size:15px;color:#6B6B6B;line-height:1.7;">
                              Tu cita en Veterinaria Santa Victoria ha sido registrada exitosamente.
                              Adjunto encontrarás el <strong>Comprobante de Cita #%d</strong> en formato PDF
                              con todos los detalles del agendamiento.
                            </p>

                            <!-- Adjunto -->
                            <div style="background:#FFF3E0;border-left:4px solid #FF9800;
                                        border-radius:8px;padding:16px 20px;margin-bottom:24px;">
                              <p style="margin:0 0 4px;font-size:13px;font-weight:700;color:#E65100;">
                                📎 Archivo adjunto
                              </p>
                              <p style="margin:0;font-size:13px;color:#6B6B6B;">
                                Comprobante_Cita_%d.pdf
                              </p>
                            </div>

                            <!-- Aviso -->
                            <div style="background:#F1F8E9;border-left:4px solid #7CB342;
                                        border-radius:8px;padding:14px 20px;margin-bottom:24px;">
                              <p style="margin:0;font-size:13px;color:#558B2F;line-height:1.6;">
                                🕐 <strong>Recuerda:</strong> Por favor asiste
                                <strong>10 minutos antes</strong> de tu cita.
                                Si necesitas cancelar o tienes alguna consulta, contáctanos por
                                WhatsApp: <strong>+51 999 888 777</strong>
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
                """.formatted(nombre, idCita, idCita);
    }

    private String construirCuerpoHtmlCancelacion(String nombreProp, String nombreMascota,
                                                   String fechaHoraFmt, String tipoCita,
                                                   String veterinario, Integer idCita) {
        String linkAgendar = baseUrl + "/cliente/citas/nueva";
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#F7F7F7;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background:#F7F7F7;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#FFFFFF;border-radius:16px;overflow:hidden;
                                    box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                        <!-- Cabecera -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#607D8B 0%%,#455A64 100%%);
                                     padding:32px 40px;text-align:center;">
                            <p style="margin:0;font-size:12px;font-weight:700;
                                      color:rgba(255,255,255,0.8);text-transform:uppercase;
                                      letter-spacing:0.1em;">
                              Veterinaria Santa Victoria
                            </p>
                            <h1 style="margin:10px 0 0;font-size:22px;font-weight:900;
                                       color:#FFFFFF;">
                              Cita Cancelada
                            </h1>
                          </td>
                        </tr>

                        <!-- Cuerpo -->
                        <tr>
                          <td style="padding:36px 40px 28px;">
                            <p style="margin:0 0 14px;font-size:16px;color:#3D3D3D;">
                              Hola, <strong>%s</strong> 👋
                            </p>
                            <p style="margin:0 0 22px;font-size:15px;color:#6B6B6B;line-height:1.7;">
                              Te confirmamos que la siguiente cita ha sido
                              <strong style="color:#E53935;">cancelada</strong>:
                            </p>

                            <!-- Detalles de la cita cancelada -->
                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="background:#F8F9FA;border:1px solid #DEE2E6;
                                          border-radius:10px;margin-bottom:24px;
                                          overflow:hidden;">
                              <tr>
                                <td style="padding:14px 20px;border-bottom:1px solid #DEE2E6;
                                           background:#FFFFFF;">
                                  <span style="font-size:11px;font-weight:700;color:#9E9E9E;
                                               text-transform:uppercase;letter-spacing:0.08em;">
                                    Número de cita
                                  </span><br>
                                  <span style="font-size:14px;font-weight:700;color:#263238;">
                                    #%d
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:14px 20px;border-bottom:1px solid #DEE2E6;">
                                  <span style="font-size:11px;font-weight:700;color:#9E9E9E;
                                               text-transform:uppercase;letter-spacing:0.08em;">
                                    Paciente
                                  </span><br>
                                  <span style="font-size:14px;color:#3D3D3D;">
                                    %s
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:14px 20px;border-bottom:1px solid #DEE2E6;
                                           background:#FFFFFF;">
                                  <span style="font-size:11px;font-weight:700;color:#9E9E9E;
                                               text-transform:uppercase;letter-spacing:0.08em;">
                                    Fecha y hora programada
                                  </span><br>
                                  <span style="font-size:14px;color:#3D3D3D;">
                                    %s
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:14px 20px;border-bottom:1px solid #DEE2E6;">
                                  <span style="font-size:11px;font-weight:700;color:#9E9E9E;
                                               text-transform:uppercase;letter-spacing:0.08em;">
                                    Tipo de consulta
                                  </span><br>
                                  <span style="font-size:14px;color:#3D3D3D;">
                                    %s
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:14px 20px;background:#FFFFFF;">
                                  <span style="font-size:11px;font-weight:700;color:#9E9E9E;
                                               text-transform:uppercase;letter-spacing:0.08em;">
                                    Veterinario asignado
                                  </span><br>
                                  <span style="font-size:14px;color:#3D3D3D;">
                                    %s
                                  </span>
                                </td>
                              </tr>
                            </table>

                            <!-- Mensaje empático -->
                            <div style="background:#FFF8E1;border-left:4px solid #FF9800;
                                        border-radius:8px;padding:18px 20px;margin-bottom:28px;">
                              <p style="margin:0;font-size:14px;color:#5D4037;line-height:1.75;">
                                Lamentamos que hayas tenido que cancelar tu cita. En SmartVet,
                                el bienestar y la salud de <strong>%s</strong> es nuestra máxima
                                prioridad. Esperamos tener una nueva oportunidad muy pronto para
                                poder ayudar y cuidar a <strong>%s</strong>. Si deseas reprogramar
                                tu atención, puedes hacerlo en cualquier momento desde nuestro portal.
                              </p>
                            </div>

                            <!-- Botón para reagendar -->
                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="margin-bottom:24px;">
                              <tr>
                                <td align="center">
                                  <a href="%s"
                                     style="display:inline-block;background:linear-gradient(135deg,#FF9800,#F57C00);
                                            color:#FFFFFF;text-decoration:none;font-size:14px;
                                            font-weight:700;padding:14px 36px;border-radius:50px;
                                            letter-spacing:0.04em;">
                                    📅 Agendar nueva cita
                                  </a>
                                </td>
                              </tr>
                            </table>

                            <hr style="border:none;border-top:1px solid #E8E8E8;margin:0 0 22px;">
                            <p style="margin:0;font-size:12px;color:#9E9E9E;text-align:center;
                                      line-height:1.6;">
                              Si tienes dudas, contáctanos al WhatsApp
                              <strong style="color:#5D4037;">+51 999 888 777</strong><br>
                              © 2025 Veterinaria Santa Victoria · Lima, Perú
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        nombreProp,
                        idCita,
                        nombreMascota,
                        fechaHoraFmt,
                        tipoCita,
                        veterinario,
                        nombreMascota,
                        nombreMascota,
                        linkAgendar);
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
