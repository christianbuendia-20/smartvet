package com.smartvet.app.controller;

import com.smartvet.app.dto.PerfilUpdateDTO;
import com.smartvet.app.exception.EmailDuplicadoException;
import com.smartvet.app.model.DireccionUsuario;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    private SmartVetUserDetails principal() {
        return (SmartVetUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
    }

    @GetMapping
    public String verPerfil(Model model) {
        Integer idUsuario = principal().getIdUsuario();
        Usuario usuario = usuarioService.buscarPorId(idUsuario);
        model.addAttribute("usuario", usuario);
        model.addAttribute("direccionActual",
                usuarioService.buscarDireccion(idUsuario).orElse(null));
        return "perfil";
    }

    @PostMapping("/actualizar")
    public String actualizarPerfil(@RequestParam String nombres,
                                    @RequestParam String apellidos,
                                    @RequestParam String email,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam(required = false) String dni,
                                    @RequestParam(required = false) String direccion,
                                    @RequestParam(required = false) String referencia,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            PerfilUpdateDTO dto = new PerfilUpdateDTO(nombres, apellidos, email, telefono, dni, direccion, referencia);
            usuarioService.actualizarPerfil(idUsuario, dto);
            log.info("Perfil actualizado por usuario_id={}", idUsuario);
            redirectAttributes.addFlashAttribute("mensajeExito", "Perfil actualizado correctamente.");
            return "redirect:/perfil";
        } catch (EmailDuplicadoException ex) {
            log.warn("Email duplicado al actualizar perfil usuario_id={}: {}", idUsuario, email);
            Usuario usuario = usuarioService.buscarPorId(idUsuario);
            model.addAttribute("usuario",          usuario);
            model.addAttribute("direccionActual",
                    usuarioService.buscarDireccion(idUsuario).orElse(null));
            model.addAttribute("errorForm",        ex.getMessage());
            model.addAttribute("nombresPrev",      nombres);
            model.addAttribute("apellidosPrev",    apellidos);
            model.addAttribute("emailPrev",        email);
            model.addAttribute("telefonoPrev",     telefono);
            model.addAttribute("dniPrev",          dni);
            model.addAttribute("direccionPrev",    direccion);
            model.addAttribute("referenciaPrev",   referencia);
            return "perfil";
        }
    }
}
