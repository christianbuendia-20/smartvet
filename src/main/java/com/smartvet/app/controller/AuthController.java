package com.smartvet.app.controller;

import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.exception.EmailDuplicadoException;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.service.UsuarioService;
import com.smartvet.app.service.VerificacionOtpService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService        usuarioService;
    private final VerificacionOtpService verificacionOtpService;

    public AuthController(UsuarioService usuarioService,
                          VerificacionOtpService verificacionOtpService) {
        this.usuarioService        = usuarioService;
        this.verificacionOtpService = verificacionOtpService;
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    // El POST /auth/login es interceptado por Spring Security antes de llegar
    // aquí. Este GET solo renderiza el formulario.

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    // ── Registro de cliente ───────────────────────────────────────────────────

    @GetMapping("/registro")
    public String mostrarRegistro(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registroForm", new UsuarioRegistroDTO("", "", "", "", "", ""));
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("registroForm") UsuarioRegistroDTO dto,
                                    BindingResult result,
                                    HttpSession session,
                                    Model model) {
        if (result.hasErrors()) {
            return "auth/registro";
        }

        Usuario usuario = null;
        try {
            usuario = usuarioService.registrarClientePendiente(dto);
            verificacionOtpService.crearYEnviar(usuario);
            session.setAttribute("pendingVerificationId", usuario.getIdUsuario());
            log.info("OTP enviado para verificación: id={}, email={}", usuario.getIdUsuario(), dto.email());
            return "redirect:/auth/verificar";
        } catch (EmailDuplicadoException ex) {
            model.addAttribute("errorRegistro", ex.getMessage());
            return "auth/registro";
        } catch (Exception ex) {
            log.error("Fallo al enviar OTP para email={}: {}", dto.email(), ex.getMessage());
            if (usuario != null) {
                usuarioService.eliminarUsuarioPendiente(usuario.getIdUsuario());
            }
            model.addAttribute("errorRegistro",
                    "No se pudo enviar el correo de verificación. Verifique su dirección de email e intente de nuevo.");
            return "auth/registro";
        }
    }

    // ── Verificación de email ─────────────────────────────────────────────────

    @GetMapping("/verificar")
    public String mostrarVerificacion(HttpSession session) {
        if (session.getAttribute("pendingVerificationId") == null) {
            return "redirect:/auth/registro";
        }
        return "auth/verificar-email";
    }

    @PostMapping("/verificar")
    public String procesarVerificacion(@RequestParam String codigo,
                                        HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        Integer idUsuario = (Integer) session.getAttribute("pendingVerificationId");
        if (idUsuario == null) {
            return "redirect:/auth/registro";
        }

        if (verificacionOtpService.verificar(idUsuario, codigo)) {
            usuarioService.activarUsuario(idUsuario);
            verificacionOtpService.eliminar(idUsuario);
            session.removeAttribute("pendingVerificationId");
            log.info("Cuenta verificada y activada: id={}", idUsuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Cuenta verificada! Ya puede iniciar sesión.");
            return "redirect:/auth/login";
        } else {
            model.addAttribute("errorOtp", "Código incorrecto. Por favor, inténtelo de nuevo.");
            return "auth/verificar-email";
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    // El POST /auth/logout es manejado por Spring Security (SecurityConfig).
    // El navbar usa un formulario POST; no se necesita un método aquí.
}
