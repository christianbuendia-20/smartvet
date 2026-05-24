package com.smartvet.app.controller;

import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Sexo;
import com.smartvet.app.service.MascotaService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    // ── Listado ───────────────────────────────────────────────────────────────

    @GetMapping
    public String listarMascotas(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        Integer idUsuario = (Integer) session.getAttribute("usuarioId");
        List<Mascota> mascotas = mascotaService.listarPorPropietario(idUsuario);

        agregarSesion(session, model);
        model.addAttribute("mascotas", mascotas);
        return "mascotas/lista";
    }

    // ── Formulario de registro ────────────────────────────────────────────────

    @GetMapping("/nueva")
    public String mostrarFormNueva(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        agregarSesion(session, model);
        model.addAttribute("sexos", Sexo.values());
        return "mascotas/nueva";
    }

    @PostMapping("/nueva")
    public String registrarMascota(@RequestParam String nombre,
                                    @RequestParam String especie,
                                    @RequestParam(required = false) String raza,
                                    @RequestParam(required = false) String color,
                                    @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
                                    @RequestParam Sexo sexo,
                                    @RequestParam(required = false) BigDecimal peso,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {

        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        Integer idUsuario = (Integer) session.getAttribute("usuarioId");

        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fechaNacimiento, sexo, peso);
            mascotaService.registrarMascota(dto, idUsuario);
            log.info("Mascota registrada por usuario_id={}: nombre='{}'", idUsuario, nombre);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡" + nombre + " fue registrado/a exitosamente!");
            return "redirect:/mascotas";
        } catch (Exception ex) {
            log.warn("Error al registrar mascota para usuario_id={}: {}", idUsuario, ex.getMessage());
            agregarSesion(session, model);
            model.addAttribute("sexos", Sexo.values());
            model.addAttribute("errorForm", ex.getMessage());
            model.addAttribute("nombrePrev", nombre);
            model.addAttribute("especiePrev", especie);
            model.addAttribute("razaPrev", raza);
            model.addAttribute("colorPrev", color);
            model.addAttribute("fechaPrev", fechaNacimiento);
            model.addAttribute("sexoPrev", sexo);
            model.addAttribute("pesoPrev", peso);
            return "mascotas/nueva";
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("nombreUsuario", session.getAttribute("usuarioNombre"));
        model.addAttribute("rolUsuario", session.getAttribute("usuarioRol"));
    }
}
