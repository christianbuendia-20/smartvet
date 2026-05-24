package com.smartvet.app.controller;

import com.smartvet.app.dto.CitaAgendamientoDTO;
import com.smartvet.app.exception.CitaNoDisponibleException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.TipoCita;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.MascotaService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final MascotaService mascotaService;

    public CitaController(CitaService citaService, MascotaService mascotaService) {
        this.citaService = citaService;
        this.mascotaService = mascotaService;
    }

    // ── Listado ───────────────────────────────────────────────────────────────

    @GetMapping
    public String listarCitas(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        Integer idUsuario = (Integer) session.getAttribute("usuarioId");
        String rol = (String) session.getAttribute("usuarioRol");

        List<Cita> citas = switch (rol) {
            case "veterinario" -> citaService.listarPorUsuarioVeterinario(idUsuario);
            default            -> citaService.listarPorPropietario(idUsuario);
        };

        agregarSesion(session, model);
        model.addAttribute("citas", citas);
        model.addAttribute("esVeterinario", "veterinario".equals(rol));
        return "citas/lista";
    }

    // ── Formulario de agendamiento ────────────────────────────────────────────

    @GetMapping("/nueva")
    public String mostrarFormNueva(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        Integer idUsuario = (Integer) session.getAttribute("usuarioId");
        List<Mascota> mascotas = mascotaService.listarPorPropietario(idUsuario);
        List<Veterinario> veterinarios = citaService.listarVeterinariosParaCita();

        agregarSesion(session, model);
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("veterinarios", veterinarios);
        model.addAttribute("tiposCita", TipoCita.values());
        return "citas/nueva";
    }

    @PostMapping("/nueva")
    public String agendarCita(@RequestParam Integer idMascota,
                               @RequestParam Integer idVeterinario,
                               @RequestParam TipoCita tipoCita,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fechaHora,
                               @RequestParam String motivo,
                               HttpSession session,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        Integer idUsuario = (Integer) session.getAttribute("usuarioId");

        try {
            CitaAgendamientoDTO dto = new CitaAgendamientoDTO(idMascota, idVeterinario, tipoCita, fechaHora, motivo);
            Cita cita = citaService.programarCita(dto);
            log.info("Cita agendada por usuario_id={}: cita_id={}", idUsuario, cita.getIdCita());
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Cita agendada exitosamente para el " +
                    fechaHora.toLocalDate() + " a las " + fechaHora.toLocalTime() + "!");
            return "redirect:/citas";
        } catch (CitaNoDisponibleException ex) {
            log.warn("Conflicto de agenda para usuario_id={}: {}", idUsuario, ex.getMessage());
            recargarFormNueva(idUsuario, session, model);
            model.addAttribute("errorForm", ex.getMessage());
            model.addAttribute("idMascotaPrev", idMascota);
            model.addAttribute("idVeterinarioxPrev", idVeterinario);
            model.addAttribute("tipoCitaPrev", tipoCita);
            model.addAttribute("fechaHoraPrev", fechaHora);
            model.addAttribute("motivoPrev", motivo);
            return "citas/nueva";
        }
    }

    // ── Cancelar ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancelar")
    public String cancelarCita(@PathVariable Integer id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        if (session.getAttribute("usuarioId") == null) return "redirect:/auth/login";

        citaService.cancelarCita(id);
        log.info("Cita id={} cancelada por usuario_id={}", id, session.getAttribute("usuarioId"));
        redirectAttributes.addFlashAttribute("mensajeExito", "Cita cancelada correctamente.");
        return "redirect:/citas";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("nombreUsuario", session.getAttribute("usuarioNombre"));
        model.addAttribute("rolUsuario", session.getAttribute("usuarioRol"));
    }

    private void recargarFormNueva(Integer idUsuario, HttpSession session, Model model) {
        agregarSesion(session, model);
        model.addAttribute("mascotas", mascotaService.listarPorPropietario(idUsuario));
        model.addAttribute("veterinarios", citaService.listarVeterinariosParaCita());
        model.addAttribute("tiposCita", TipoCita.values());
    }
}
