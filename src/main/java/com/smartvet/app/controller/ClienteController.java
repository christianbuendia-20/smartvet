package com.smartvet.app.controller;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.smartvet.app.dto.CitaAgendamientoDTO;
import com.smartvet.app.dto.HistoriaClinicaUpdateDTO;
import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.exception.CitaNoDisponibleException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Consulta;
import com.smartvet.app.model.HistoriaClinica;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Sexo;
import com.smartvet.app.model.TipoCita;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.CitaPdfService;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.ConsultaPdfService;
import com.smartvet.app.service.ConsultaService;
import com.smartvet.app.service.EmailService;
import com.smartvet.app.service.FileStorageService;
import com.smartvet.app.service.MascotaService;
import com.smartvet.app.service.MercadoPagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.time.LocalTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final CitaService         citaService;
    private final MascotaService      mascotaService;
    private final ConsultaService     consultaService;
    private final ConsultaPdfService  consultaPdfService;
    private final CitaPdfService      citaPdfService;
    private final EmailService        emailService;
    private final FileStorageService  fileStorageService;
    private final MercadoPagoService  mercadoPagoService;
    private final String              mpPublicKey;

    public ClienteController(CitaService citaService,
                              MascotaService mascotaService,
                              ConsultaService consultaService,
                              ConsultaPdfService consultaPdfService,
                              CitaPdfService citaPdfService,
                              EmailService emailService,
                              FileStorageService fileStorageService,
                              MercadoPagoService mercadoPagoService,
                              @Value("${mercadopago.public-key}") String mpPublicKey) {
        this.citaService        = citaService;
        this.mascotaService     = mascotaService;
        this.consultaService    = consultaService;
        this.consultaPdfService = consultaPdfService;
        this.citaPdfService     = citaPdfService;
        this.emailService       = emailService;
        this.fileStorageService = fileStorageService;
        this.mercadoPagoService = mercadoPagoService;
        this.mpPublicKey        = mpPublicKey;
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

    // ── Pagos pendientes del cliente ─────────────────────────────────────────

    @GetMapping("/mis-pagos")
    public String misPagosPendientes(Model model) {
        Integer idUsuario = principal().getIdUsuario();
        List<Cita> citasPendientes =
                citaService.listarCitasPendientesPagoPorPropietario(idUsuario);
        model.addAttribute("citasPendientes", citasPendientes);
        model.addAttribute("mpPublicKey",     mpPublicKey);
        return "cliente/mis-pagos";
    }

    // ── Historial completo de citas ───────────────────────────────────────────

    @GetMapping("/citas")
    public String listarMisCitas(@RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "fecha", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  Model model) {
        Integer idUsuario = principal().getIdUsuario();
        List<Cita> citas = citaService.listarPorPropietario(idUsuario, keyword, fecha);
        model.addAttribute("citas",   citas);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("fecha",   fecha);
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
                                    @RequestParam(required = false) String alergias,
                                    @RequestParam(required = false) String enfermedadesCronicas,
                                    @RequestParam(required = false) String notasGenerales,
                                    @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {

        Integer idUsuario = principal().getIdUsuario();
        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fechaNacimiento, sexo, peso);
            Mascota mascota = mascotaService.registrarMascota(dto, idUsuario);

            if (archivo != null && !archivo.isEmpty()) {
                String nombreFoto = fileStorageService.guardarFotoMascota(archivo);
                mascotaService.actualizarFoto(mascota.getIdMascota(), nombreFoto);
            }

            boolean tieneHistoria = (alergias != null && !alergias.isBlank())
                    || (enfermedadesCronicas != null && !enfermedadesCronicas.isBlank())
                    || (notasGenerales != null && !notasGenerales.isBlank());
            if (tieneHistoria) {
                mascotaService.actualizarHistoriaClinica(mascota.getIdMascota(),
                        new HistoriaClinicaUpdateDTO(alergias, enfermedadesCronicas, notasGenerales));
            }

            log.info("Mascota registrada por cliente usuario_id={}: mascota_id={}, nombre='{}'",
                    idUsuario, mascota.getIdMascota(), nombre);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Mascota \"" + nombre + "\" registrada correctamente!");
            return "redirect:/cliente/dashboard";
        } catch (Exception ex) {
            log.error("Error al registrar mascota para cliente usuario_id={}: {}", idUsuario, ex.getMessage());
            model.addAttribute("sexos", Sexo.values());
            model.addAttribute("errorForm", "No se pudo registrar la mascota: " + ex.getMessage());
            model.addAttribute("nombrePrev",          nombre);
            model.addAttribute("especiePrev",         especie);
            model.addAttribute("razaPrev",            raza);
            model.addAttribute("colorPrev",           color);
            model.addAttribute("fechaNacimientoPrev", fechaNacimiento);
            model.addAttribute("sexoPrev",            sexo);
            model.addAttribute("pesoPrev",            peso);
            model.addAttribute("alergiaPrev",         alergias);
            model.addAttribute("enfermedadesPrev",    enfermedadesCronicas);
            model.addAttribute("notasPrev",           notasGenerales);
            return "cliente/nueva-mascota";
        }
    }

    // ── Editar datos básicos de mascota ───────────────────────────────────────

    @GetMapping("/mascotas/{id}/editar")
    public String mostrarFormEditarMascota(@PathVariable Integer id,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            Mascota mascota = mascotaService.buscarPorId(id);
            if (!mascota.getPropietario().getIdUsuario().equals(idUsuario)) {
                redirectAttributes.addFlashAttribute("errorAcceso",
                        "No tienes permiso para editar esta mascota.");
                return "redirect:/cliente/mascotas";
            }
            model.addAttribute("mascota", mascota);
            model.addAttribute("sexos",   Sexo.values());
            return "cliente/editar-mascota";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/cliente/mascotas";
        }
    }

    @PostMapping("/mascotas/{id}/editar")
    public String editarMascota(@PathVariable Integer id,
                                 @RequestParam String nombre,
                                 @RequestParam String especie,
                                 @RequestParam(required = false) String raza,
                                 @RequestParam(required = false) String color,
                                 @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaNacimiento,
                                 @RequestParam Sexo sexo,
                                 @RequestParam(required = false) BigDecimal peso,
                                 @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fechaNacimiento, sexo, peso);
            mascotaService.actualizarMascota(id, dto);

            // actualizarMascota solo pisa campos del DTO; rutaFoto queda intacta en BD.
            // Solo llamamos actualizarFoto cuando el usuario subió una imagen nueva.
            if (archivo != null && !archivo.isEmpty()) {
                String nombreFoto = fileStorageService.guardarFotoMascota(archivo);
                mascotaService.actualizarFoto(id, nombreFoto);
            }

            log.info("Mascota id={} actualizada por cliente usuario_id={}", id, idUsuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Datos de \"" + nombre + "\" actualizados correctamente.");
            return "redirect:/cliente/mascotas/" + id + "/historial";
        } catch (Exception ex) {
            log.error("Error al actualizar mascota id={} por cliente usuario_id={}: {}",
                    id, idUsuario, ex.getMessage());
            model.addAttribute("errorForm", "No se pudo actualizar: " + ex.getMessage());
            try { model.addAttribute("mascota", mascotaService.buscarPorId(id)); }
            catch (Exception ignored) {}
            model.addAttribute("sexos", Sexo.values());
            return "cliente/editar-mascota";
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

        if (citaService.contarCitasActivasPorPropietario(idUsuario) >= 3) {
            redirectAttributes.addFlashAttribute("error",
                    "Límite de citas alcanzado: Ya tienes 3 citas activas programadas. " +
                    "Para agendar una nueva, debes cancelar una cita existente.");
            return "redirect:/cliente/citas/nueva";
        }

        if (!fechaHora.toLocalDate().isAfter(LocalDate.now())) {
            recargarForm(idUsuario, model);
            model.addAttribute("errorForm",          "Las citas deben agendarse con al menos un día de anticipación.");
            model.addAttribute("idMascotaPrev",      idMascota);
            model.addAttribute("idVeterinarioxPrev", idVeterinario);
            model.addAttribute("tipoCitaPrev",       tipoCita);
            model.addAttribute("fechaHoraPrev",      fechaHora);
            model.addAttribute("motivoPrev",         motivo);
            return "cliente/nueva-cita";
        }

        LocalTime hora = fechaHora.toLocalTime();
        if (hora.isBefore(LocalTime.of(9, 0)) || hora.isAfter(LocalTime.of(19, 0))) {
            recargarForm(idUsuario, model);
            model.addAttribute("errorForm",          "El horario de atención es de 09:00 a 19:00 horas.");
            model.addAttribute("idMascotaPrev",      idMascota);
            model.addAttribute("idVeterinarioxPrev", idVeterinario);
            model.addAttribute("tipoCitaPrev",       tipoCita);
            model.addAttribute("fechaHoraPrev",      fechaHora);
            model.addAttribute("motivoPrev",         motivo);
            return "cliente/nueva-cita";
        }

        LocalDateTime fechaMaxima = LocalDateTime.now().plusDays(15);
        if (fechaHora.isAfter(fechaMaxima)) {
            recargarForm(idUsuario, model);
            model.addAttribute("errorForm",          "No se pueden programar citas con más de 15 días de anticipación.");
            model.addAttribute("idMascotaPrev",      idMascota);
            model.addAttribute("idVeterinarioxPrev", idVeterinario);
            model.addAttribute("tipoCitaPrev",       tipoCita);
            model.addAttribute("fechaHoraPrev",      fechaHora);
            model.addAttribute("motivoPrev",         motivo);
            return "cliente/nueva-cita";
        }

        try {
            CitaAgendamientoDTO dto = new CitaAgendamientoDTO(
                    idMascota, idVeterinario, tipoCita, fechaHora, motivo);
            Cita cita = citaService.programarCita(dto);
            log.info("Cita agendada por cliente usuario_id={}: cita_id={}", idUsuario, cita.getIdCita());

            try {
                byte[] pdfBytes = citaPdfService.generarPdf(cita.getIdCita());
                emailService.enviarComprobanteCita(
                        principal().getUsername(),
                        principal().getNombreCompleto(),
                        pdfBytes,
                        cita.getIdCita());
                log.info("Comprobante enviado por email: cita_id={}", cita.getIdCita());
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "¡Cita confirmada para el " + fechaHora.toLocalDate()
                        + " a las " + fechaHora.toLocalTime()
                        + "! Se ha enviado un comprobante PDF a tu correo electrónico.");
            } catch (Exception ex) {
                log.warn("No se pudo enviar el comprobante por email: cita_id={}", cita.getIdCita(), ex);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "¡Cita confirmada para el " + fechaHora.toLocalDate()
                        + " a las " + fechaHora.toLocalTime() + "!");
            }

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
        emailService.enviarCancelacionCita(id);
        redirectAttributes.addFlashAttribute("mensajeExito", "Cita cancelada correctamente.");
        return "redirect:/cliente/dashboard";
    }

    // ── Historial clínico de mascota ──────────────────────────────────────────

    @GetMapping("/mascotas/{idMascota}/historial")
    public String verHistorial(@PathVariable Integer idMascota,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            Mascota mascota = mascotaService.buscarPorId(idMascota);
            if (!mascota.getPropietario().getIdUsuario().equals(idUsuario)) {
                log.warn("Cliente usuario_id={} intentó acceder al historial de mascota_id={} ajena",
                        idUsuario, idMascota);
                redirectAttributes.addFlashAttribute("errorAcceso",
                        "No tienes permiso para ver el historial de esta mascota.");
                return "redirect:/cliente/mascotas";
            }
            HistoriaClinica historia = mascotaService.verHistorialClinico(idMascota);
            List<Consulta> consultas = consultaService.listarPorMascota(idMascota);
            model.addAttribute("mascota",   mascota);
            model.addAttribute("historia",  historia);
            model.addAttribute("consultas", consultas);
            return "cliente/historial";
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Historial no disponible para mascota_id={}: {}", idMascota, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/cliente/mascotas";
        }
    }

    // ── Detalle y descarga de consulta ────────────────────────────────────────

    @GetMapping("/consultas/por-cita/{idCita}")
    public String redirigirAConsulta(@PathVariable Integer idCita,
                                      RedirectAttributes redirectAttributes) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            Consulta consulta = consultaService.buscarPorCita(idCita);
            if (!consulta.getMascota().getPropietario().getIdUsuario().equals(idUsuario)) {
                redirectAttributes.addFlashAttribute("errorAcceso",
                        "No tienes permiso para ver esta consulta.");
                return "redirect:/cliente/citas";
            }
            return "redirect:/cliente/consultas/" + consulta.getIdConsulta();
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso",
                    "No se encontró un registro médico para esta cita.");
            return "redirect:/cliente/citas";
        }
    }

    @GetMapping("/consultas/{id}")
    public String verConsulta(@PathVariable Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            Consulta consulta = consultaService.buscarPorId(id);
            if (!consulta.getMascota().getPropietario().getIdUsuario().equals(idUsuario)) {
                log.warn("Cliente usuario_id={} intentó acceder a consulta_id={} de otra mascota",
                        idUsuario, id);
                redirectAttributes.addFlashAttribute("errorAcceso",
                        "No tienes permiso para ver esta consulta.");
                return "redirect:/cliente/citas";
            }
            model.addAttribute("consulta", consulta);
            return "cliente/ver-consulta";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/cliente/citas";
        }
    }

    @GetMapping("/consultas/{id}/pdf/descargar")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        Integer idUsuario = principal().getIdUsuario();
        try {
            Consulta consulta = consultaService.buscarPorId(id);
            if (!consulta.getMascota().getPropietario().getIdUsuario().equals(idUsuario)) {
                log.warn("Cliente usuario_id={} intentó descargar PDF de consulta_id={} ajena",
                        idUsuario, id);
                return ResponseEntity.status(403).build();
            }
            byte[] pdf = consultaPdfService.generarPdf(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"Consulta_" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RecursoNoEncontradoException ex) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException ex) {
            log.error("Error al generar PDF consulta_id={}", id, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Cartilla de vacunación ────────────────────────────────────────────────

    @GetMapping("/vacunas")
    public String cartillaVacunacion(
            @RequestParam(required = false) Integer idMascota,
            Model model) {
        Integer idUsuario = principal().getIdUsuario();
        List<Mascota> mascotas = Objects.requireNonNullElse(
                mascotaService.listarPorPropietario(idUsuario), Collections.emptyList());

        // Si no se seleccionó mascota, usar la primera de la lista
        Integer idSeleccionada = idMascota;
        if (idSeleccionada == null && !mascotas.isEmpty()) {
            idSeleccionada = mascotas.get(0).getIdMascota();
        }

        model.addAttribute("mascotas",              mascotas);
        model.addAttribute("idMascotaSeleccionada", idSeleccionada);
        // Las listas vacías evitan NullPointerException en Thymeleaf
        model.addAttribute("tratamientos", Collections.emptyList());
        model.addAttribute("vacunas",      Collections.emptyList());
        return "cliente/cartilla";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void recargarForm(Integer idUsuario, Model model) {
        model.addAttribute("mascotas",     mascotaService.listarPorPropietario(idUsuario));
        model.addAttribute("veterinarios", citaService.listarVeterinariosParaCita());
        model.addAttribute("tiposCita",    TipoCita.values());
    }
}
