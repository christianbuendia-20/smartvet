package com.smartvet.app.controller;

import com.smartvet.app.dto.CitaAgendamientoDTO;
import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.exception.CitaNoDisponibleException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Sexo;
import com.smartvet.app.model.TipoCita;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.MascotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final CitaService    citaService;
    private final MascotaService mascotaService;

    public ClienteController(CitaService citaService, MascotaService mascotaService) {
        this.citaService    = citaService;
        this.mascotaService = mascotaService;
    }

    private SmartVetUserDetails principal() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(p instanceof SmartVetUserDetails details)) {
            throw new IllegalStateException("Principal inesperado: " + p.getClass().getName());
        }
        return details;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Integer idUsuario = principal().getIdUsuario();

        List<Mascota> mascotas    = Objects.requireNonNullElse(
                mascotaService.listarPorPropietario(idUsuario), Collections.emptyList());
        List<Cita> proximasCitas  = Objects.requireNonNullElse(
                citaService.listarProximasCitasPorPropietario(idUsuario), Collections.emptyList());
        long historicas           = citaService.contarCitasHistoricas(idUsuario);

        model.addAttribute("mascotas",        mascotas);
        model.addAttribute("totalMascotas",   mascotas.size());
        model.addAttribute("proximasCitas",   proximasCitas);
        model.addAttribute("totalProximas",   proximasCitas.size());
        model.addAttribute("totalHistoricas", historicas);
        return "cliente/dashboard";
    }

    // ── Historial completo de citas ───────────────────────────────────────────

    @GetMapping("/citas")
    public String listarMisCitas(Model model) {
        Integer idUsuario = principal().getIdUsuario();
        List<Cita> citas = Objects.requireNonNullElse(
                citaService.listarPorPropietario(idUsuario), Collections.emptyList());
        model.addAttribute("citas", citas);
        return "cliente/mis-citas";
    }

    // ── Lista paginada de mascotas ────────────────────────────────────────────

    @GetMapping("/mascotas")
    public String listarMascotas(@RequestParam(defaultValue = "0") int page,
                                  Model model) {
        Integer idUsuario = principal().getIdUsuario();
        PageRequest pageable = PageRequest.of(page, 8, Sort.by("nombre").ascending());
        Page<Mascota> paginaMascotas = mascotaService.listarPorPropietarioPaginado(idUsuario, pageable);
        model.addAttribute("mascotas",     paginaMascotas.getContent());
        model.addAttribute("pagina",       paginaMascotas);
        return "cliente/mascotas";
    }

    // ── Registro de mascota ───────────────────────────────────────────────────

    @GetMapping("/mascotas/nueva")
    public String mostrarFormNuevaMascota(Model model) {
        model.addAttribute("sexos", Sexo.values());
        return "cliente/nueva-mascota";
    }

    @PostMapping("/mascotas/nueva")
    public String registrarMascota(@RequestParam String nombre,
                                    @RequestParam String especie,
                                    @RequestParam(required = false) String raza,
                                    @RequestParam(required = false) String color,
                                    @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
                                    @RequestParam Sexo sexo,
                                    @RequestParam(required = false) BigDecimal peso,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {

        Integer idUsuario = principal().getIdUsuario();
        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fechaNacimiento, sexo, peso);
            Mascota mascota = mascotaService.registrarMascota(dto, idUsuario);
            log.info("Mascota registrada por cliente usuario_id={}: mascota_id={}, nombre='{}'",
                    idUsuario, mascota.getIdMascota(), nombre);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Mascota \"" + nombre + "\" registrada correctamente!");
            return "redirect:/cliente/dashboard";
        } catch (Exception ex) {
            log.error("Error al registrar mascota para cliente usuario_id={}: {}", idUsuario, ex.getMessage());
            model.addAttribute("sexos", Sexo.values());
            model.addAttribute("errorForm", "No se pudo registrar la mascota: " + ex.getMessage());
            model.addAttribute("nombrePrev",        nombre);
            model.addAttribute("especiePrev",       especie);
            model.addAttribute("razaPrev",          raza);
            model.addAttribute("colorPrev",         color);
            model.addAttribute("fechaNacimientoPrev", fechaNacimiento);
            model.addAttribute("sexoPrev",          sexo);
            model.addAttribute("pesoPrev",          peso);
            return "cliente/nueva-mascota";
        }
    }

    // ── Agendamiento de cita ──────────────────────────────────────────────────

    @GetMapping("/citas/nueva")
    public String mostrarFormNuevaCita(Model model) {
        Integer idUsuario = principal().getIdUsuario();

        model.addAttribute("mascotas",    mascotaService.listarPorPropietario(idUsuario));
        model.addAttribute("veterinarios", citaService.listarVeterinariosParaCita());
        model.addAttribute("tiposCita",   TipoCita.values());
        return "cliente/nueva-cita";
    }

    @PostMapping("/citas/nueva")
    public String agendarCita(@RequestParam Integer idMascota,
                               @RequestParam Integer idVeterinario,
                               @RequestParam TipoCita tipoCita,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fechaHora,
                               @RequestParam String motivo,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        Integer idUsuario = principal().getIdUsuario();

        try {
            CitaAgendamientoDTO dto = new CitaAgendamientoDTO(
                    idMascota, idVeterinario, tipoCita, fechaHora, motivo);
            Cita cita = citaService.programarCita(dto);
            log.info("Cita agendada por cliente usuario_id={}: cita_id={}", idUsuario, cita.getIdCita());
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Cita confirmada para el "
                    + fechaHora.toLocalDate() + " a las " + fechaHora.toLocalTime() + "!");
            return "redirect:/cliente/dashboard";

        } catch (CitaNoDisponibleException ex) {
            log.warn("Conflicto al agendar cita para cliente usuario_id={}: {}", idUsuario, ex.getMessage());
            recargarForm(idUsuario, model);
            model.addAttribute("errorForm",         ex.getMessage());
            model.addAttribute("idMascotaPrev",     idMascota);
            model.addAttribute("idVeterinarioxPrev", idVeterinario);
            model.addAttribute("tipoCitaPrev",      tipoCita);
            model.addAttribute("fechaHoraPrev",     fechaHora);
            model.addAttribute("motivoPrev",        motivo);
            return "cliente/nueva-cita";
        }
    }

    // ── Cancelar desde el dashboard ───────────────────────────────────────────

    @PostMapping("/citas/{id}/cancelar")
    public String cancelarCita(@PathVariable Integer id,
                                RedirectAttributes redirectAttributes) {

        citaService.cancelarCita(id);
        log.info("Cita id={} cancelada por cliente usuario_id={}", id, principal().getIdUsuario());
        redirectAttributes.addFlashAttribute("mensajeExito", "Cita cancelada correctamente.");
        return "redirect:/cliente/dashboard";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void recargarForm(Integer idUsuario, Model model) {
        model.addAttribute("mascotas",     mascotaService.listarPorPropietario(idUsuario));
        model.addAttribute("veterinarios", citaService.listarVeterinariosParaCita());
        model.addAttribute("tiposCita",    TipoCita.values());
    }
}
