package com.smartvet.app.controller;

import com.smartvet.app.dto.ConsultaMedicaDTO;
import com.smartvet.app.dto.DetalleRecetaDTO;
import com.smartvet.app.dto.RecetaDTO;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Consulta;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.HistoriaClinica;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Producto;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.ConsultaService;
import com.smartvet.app.service.MascotaService;
import com.smartvet.app.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/vet")
public class VeterinarioController {

    private final CitaService     citaService;
    private final ConsultaService consultaService;
    private final MascotaService  mascotaService;
    private final ProductoService productoService;

    public VeterinarioController(CitaService citaService,
                                  ConsultaService consultaService,
                                  MascotaService mascotaService,
                                  ProductoService productoService) {
        this.citaService     = citaService;
        this.consultaService = consultaService;
        this.mascotaService  = mascotaService;
        this.productoService = productoService;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SmartVetUserDetails principal() {
        return (SmartVetUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        SmartVetUserDetails details = principal();
        List<Cita> citasHoy      = citaService.listarCitasDeHoyPorVeterinario(details.getIdUsuario());
        List<Cita> proximasCitas = citaService.listarProximasCitasPorVeterinario(details.getIdUsuario());

        model.addAttribute("citasHoy",         citasHoy);
        model.addAttribute("totalCitasHoy",    citasHoy.size());
        model.addAttribute("totalPendientes",  citaService.contarPendientes(citasHoy));
        model.addAttribute("totalEnCurso",     citaService.contarPorEstado(citasHoy, EstadoCita.EN_CURSO));
        model.addAttribute("totalCompletadas", citaService.contarPorEstado(citasHoy, EstadoCita.COMPLETADA));
        model.addAttribute("proximasCitas",    proximasCitas);
        return "vet/dashboard";
    }

    // ── Agenda completa ───────────────────────────────────────────────────────

    @GetMapping("/agenda")
    public String agenda(Model model) {
        SmartVetUserDetails details = principal();
        List<Cita> misCitas = citaService.listarAgendaPorVeterinario(details.getIdUsuario());
        model.addAttribute("misCitas", misCitas);
        return "vet/agenda";
    }

    // ── Historial clínico ─────────────────────────────────────────────────────

    @GetMapping("/historial/{idMascota}")
    public String verHistorial(@PathVariable Integer idMascota,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            Mascota mascota          = mascotaService.buscarPorId(idMascota);
            HistoriaClinica historia = mascotaService.verHistorialClinico(idMascota);
            List<Consulta> consultas = consultaService.listarPorMascota(idMascota);

            model.addAttribute("mascota",   mascota);
            model.addAttribute("historia",  historia);
            model.addAttribute("consultas", consultas);
            return "vet/historial";
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Historial no disponible para mascota_id={}: {}", idMascota, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/vet/dashboard";
        }
    }

    // ── Consulta médica ───────────────────────────────────────────────────────

    @GetMapping("/consulta/{idCita}")
    public String mostrarFormConsulta(@PathVariable Integer idCita,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        SmartVetUserDetails details = principal();
        Cita cita = citaService.buscarPorId(idCita);

        if (!cita.getVeterinario().getUsuario().getIdUsuario().equals(details.getIdUsuario())) {
            log.warn("Vet usuario_id={} intentó acceder a cita_id={} de otro veterinario",
                    details.getIdUsuario(), idCita);
            redirectAttributes.addFlashAttribute("errorAcceso",
                    "No tienes permisos para registrar esta consulta.");
            return "redirect:/vet/dashboard";
        }

        try {
            consultaService.buscarPorCita(idCita);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Esta cita ya tiene una consulta registrada.");
            return "redirect:/vet/dashboard";
        } catch (RecursoNoEncontradoException ignored) {
            // No existe consulta previa: mostramos el formulario
        }

        List<Producto> productos = productoService.listarActivos();
        model.addAttribute("cita",     cita);
        model.addAttribute("productos", productos);
        return "vet/consulta";
    }

    @PostMapping("/consulta/{idCita}")
    public String registrarConsulta(@PathVariable Integer idCita,
                                     @RequestParam String diagnostico,
                                     @RequestParam(required = false) String tratamiento,
                                     @RequestParam(required = false) String observaciones,
                                     @RequestParam(required = false) BigDecimal temperatura,
                                     @RequestParam(required = false) BigDecimal peso,
                                     @RequestParam(required = false) Integer frecuenciaCardiaca,
                                     @RequestParam(required = false) String incluirReceta,
                                     @RequestParam(required = false) String instruccionesReceta,
                                     @RequestParam(required = false) Integer vigenciaDias,
                                     @RequestParam(required = false) Integer[] idProducto,
                                     @RequestParam(required = false) Integer[] cantidad,
                                     @RequestParam(required = false) String[] dosis,
                                     @RequestParam(required = false) String[] frecuencia,
                                     @RequestParam(required = false) Integer[] duracionDias,
                                     @RequestParam(required = false) String[] instruccionesAdicionales,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {

        SmartVetUserDetails details = principal();

        try {
            RecetaDTO receta = construirReceta(
                    incluirReceta, instruccionesReceta, vigenciaDias,
                    idProducto, cantidad, dosis, frecuencia, duracionDias, instruccionesAdicionales);

            ConsultaMedicaDTO dto = new ConsultaMedicaDTO(
                    idCita, LocalDateTime.now(),
                    temperatura, peso, frecuenciaCardiaca,
                    diagnostico, tratamiento, observaciones,
                    receta);

            consultaService.registrarConsulta(dto);
            log.info("Consulta registrada: vet_usuario_id={}, cita_id={}, receta={}",
                    details.getIdUsuario(), idCita, receta != null);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Consulta registrada exitosamente. Cita marcada como completada.");
            return "redirect:/vet/dashboard";

        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error al registrar consulta para cita_id={}: {}", idCita, ex.getMessage());
            model.addAttribute("errorForm",         ex.getMessage());
            model.addAttribute("cita",              citaService.buscarPorId(idCita));
            model.addAttribute("productos",         productoService.listarActivos());
            model.addAttribute("diagnosticoPrev",   diagnostico);
            model.addAttribute("tratamientoPrev",   tratamiento);
            model.addAttribute("observacionesPrev", observaciones);
            return "vet/consulta";
        }
        // StockInsuficienteException propagates to @ControllerAdvice → error/error (HTTP 409)
    }

    // ── Helper privado ────────────────────────────────────────────────────────

    private RecetaDTO construirReceta(String incluirReceta,
                                      String instrucciones,
                                      Integer vigenciaDias,
                                      Integer[] idProducto,
                                      Integer[] cantidad,
                                      String[] dosis,
                                      String[] frecuencia,
                                      Integer[] duracionDias,
                                      String[] instruccionesAdicionales) {
        if (incluirReceta == null || idProducto == null || idProducto.length == 0) {
            return null;
        }

        List<DetalleRecetaDTO> detalles = new ArrayList<>();
        for (int i = 0; i < idProducto.length; i++) {
            if (idProducto[i] == null) continue;
            detalles.add(new DetalleRecetaDTO(
                    idProducto[i],
                    at(cantidad, i, 1),
                    at(dosis, i),
                    at(frecuencia, i),
                    at(duracionDias, i),
                    at(instruccionesAdicionales, i)
            ));
        }

        return detalles.isEmpty() ? null : new RecetaDTO(instrucciones, vigenciaDias, detalles);
    }

    private <T> T at(T[] arr, int i, T fallback) {
        return (arr != null && i < arr.length && arr[i] != null) ? arr[i] : fallback;
    }

    private String at(String[] arr, int i) {
        return (arr != null && i < arr.length) ? arr[i] : null;
    }

    private Integer at(Integer[] arr, int i) {
        return (arr != null && i < arr.length) ? arr[i] : null;
    }
}
