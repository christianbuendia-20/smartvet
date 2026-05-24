package com.smartvet.app.controller;

import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.exception.EmailDuplicadoException;
import com.smartvet.app.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
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
    public String mostrarRegistro(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/dashboard";
        }
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String email,
                                    @RequestParam String password,
                                    @RequestParam String nombres,
                                    @RequestParam String apellidos,
                                    @RequestParam(required = false) String dni,
                                    @RequestParam(required = false) String telefono,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            usuarioService.registrarCliente(new UsuarioRegistroDTO(email, password, nombres, apellidos, dni, telefono));
            log.info("Nuevo cliente registrado con email={}", email);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Cuenta creada exitosamente! Ya puede iniciar sesión.");
            return "redirect:/auth/login";
        } catch (EmailDuplicadoException ex) {
            model.addAttribute("errorRegistro", ex.getMessage());
            model.addAttribute("emailPrevio", email);
            model.addAttribute("nombresPrevio", nombres);
            model.addAttribute("apellidosPrevio", apellidos);
            model.addAttribute("dniPrevio", dni);
            model.addAttribute("telefonoPrevio", telefono);
            return "auth/registro";
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    // El POST /auth/logout es manejado por Spring Security (SecurityConfig).
    // El navbar usa un formulario POST; no se necesita un método aquí.
}
