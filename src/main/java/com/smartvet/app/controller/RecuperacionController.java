package com.smartvet.app.controller;

import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.service.UsuarioService;
import com.smartvet.app.service.VerificacionOtpService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
public class RecuperacionController {

    private static final String SESSION_ID       = "recuperacionUsuarioId";
    private static final String SESSION_VERIFIED = "recuperacionVerificada";

    private final UsuarioService         usuarioService;
    private final VerificacionOtpService verificacionOtpService;

    public RecuperacionController(UsuarioService usuarioService,
                                   VerificacionOtpService verificacionOtpService) {
        this.usuarioService        = usuarioService;
        this.verificacionOtpService = verificacionOtpService;
    }

    // ── Paso 1: Solicitud de recuperación ─────────────────────────────────────

    @GetMapping("/recuperar-password")
    public String mostrarSolicitud() {
        return "auth/recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String procesarSolicitud(@RequestParam String email,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.buscarPorEmail(email.trim().toLowerCase());
            verificacionOtpService.crearYEnviarRecuperacion(usuario);
            session.setAttribute(SESSION_ID, usuario.getIdUsuario());
            log.info("OTP de recuperación enviado: usuario_id={}", usuario.getIdUsuario());
        } catch (RecursoNoEncontradoException ex) {
            // No revelar si el correo está o no registrado
            log.warn("Solicitud de recuperación para email no registrado: {}", email);
        } catch (RuntimeException ex) {
            log.error("Fallo al enviar correo de recuperación para email={}: {}", email, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm",
                    "No se pudo enviar el correo. Verifique su dirección e intente de nuevo.");
            return "redirect:/recuperar-password";
        }
        // Siempre redirigir al paso 2 para no confirmar si el email existe
        redirectAttributes.addFlashAttribute("emailEnviado", email.trim().toLowerCase());
        return "redirect:/verificar-recuperacion";
    }

    // ── Paso 2: Verificación del código OTP ───────────────────────────────────

    @GetMapping("/verificar-recuperacion")
    public String mostrarVerificacion(HttpSession session, Model model) {
        if (session.getAttribute(SESSION_ID) == null) {
            return "redirect:/recuperar-password";
        }
        return "auth/verificar-recuperacion";
    }

    @PostMapping("/verificar-recuperacion")
    public String procesarVerificacion(@RequestParam String codigo,
                                        HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        Integer idUsuario = (Integer) session.getAttribute(SESSION_ID);
        if (idUsuario == null) {
            return "redirect:/recuperar-password";
        }

        if (verificacionOtpService.verificar(idUsuario, codigo)) {
            session.setAttribute(SESSION_VERIFIED, Boolean.TRUE);
            log.info("Código de recuperación verificado: usuario_id={}", idUsuario);
            return "redirect:/restablecer-password";
        }

        log.warn("Código de recuperación incorrecto para usuario_id={}", idUsuario);
        model.addAttribute("errorOtp", "Código incorrecto. Por favor, inténtalo de nuevo.");
        return "auth/verificar-recuperacion";
    }

    // ── Paso 3: Nueva contraseña ───────────────────────────────────────────────

    @GetMapping("/restablecer-password")
    public String mostrarNuevaContrasena(HttpSession session) {
        if (session.getAttribute(SESSION_ID) == null
                || !Boolean.TRUE.equals(session.getAttribute(SESSION_VERIFIED))) {
            return "redirect:/recuperar-password";
        }
        return "auth/restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String procesarNuevaContrasena(@RequestParam String nuevaContrasena,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        Integer idUsuario = (Integer) session.getAttribute(SESSION_ID);
        if (idUsuario == null || !Boolean.TRUE.equals(session.getAttribute(SESSION_VERIFIED))) {
            return "redirect:/recuperar-password";
        }

        if (nuevaContrasena == null || nuevaContrasena.trim().length() < 8) {
            redirectAttributes.addFlashAttribute("errorForm",
                    "La contraseña debe tener al menos 8 caracteres.");
            return "redirect:/restablecer-password";
        }

        try {
            usuarioService.restablecerContrasena(idUsuario, nuevaContrasena);
            verificacionOtpService.eliminar(idUsuario);
            session.removeAttribute(SESSION_ID);
            session.removeAttribute(SESSION_VERIFIED);
            log.info("Contraseña restablecida exitosamente: usuario_id={}", idUsuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Contraseña actualizada correctamente. Ya puedes ingresar con tus nuevos datos.");
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", "Sesión inválida. Intenta nuevamente.");
            return "redirect:/recuperar-password";
        }

        return "redirect:/auth/login";
    }
}
