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
import com.smartvet.app.model.Usuario;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.ConsultaPdfService;
import com.smartvet.app.service.ConsultaService;
import com.smartvet.app.service.EmailService;
import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.model.Sexo;
import com.smartvet.app.service.FileStorageService;
import com.smartvet.app.service.MascotaService;
import com.smartvet.app.service.ProductoService;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/vet")
public class VeterinarioController {

    private final CitaService        citaService;
    private final ConsultaService    consultaService;
    private final MascotaService     mascotaService;
    private final ProductoService    productoService;
    private final ConsultaPdfService consultaPdfService;
    private final EmailService       emailService;
    private final FileStorageService fileStorageService;

    public VeterinarioController(CitaService citaService,
                                  ConsultaService consultaService,
                                  MascotaService mascotaService,
                                  ProductoService productoService,
                                  ConsultaPdfService consultaPdfService,
                                  EmailService emailService,
                                  FileStorageService fileStorageService) {
        this.citaService         = citaService;
        this.consultaService     = consultaService;
        this.mascotaService      = mascotaService;
        this.productoService     = productoService;
        this.consultaPdfService  = consultaPdfService;
        this.emailService        = emailService;
        this.fileStorageService  = fileStorageService;
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
    public String agenda(@RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "fecha", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                         Model model) {
        SmartVetUserDetails details = principal();
        List<Cita> misCitas = citaService.listarAgendaPorVeterinario(details.getIdUsuario(), keyword, fecha);
        model.addAttribute("misCitas", misCitas);
        model.addAttribute("keyword",  keyword != null ? keyword : "");
        model.addAttribute("fecha",    fecha);
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

            Consulta nueva = consultaService.registrarConsulta(dto);
            log.info("Consulta registrada: vet_usuario_id={}, cita_id={}, consulta_id={}, receta={}",
                    details.getIdUsuario(), idCita, nueva.getIdConsulta(), receta != null);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Consulta guardada correctamente. Puedes descargar o enviar el informe PDF desde esta vista.");
            return "redirect:/vet/consultas/" + nueva.getIdConsulta();

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

    // ── Detalle, edición y exportación de consulta ───────────────────────────

    @GetMapping("/consultas/{id}")
    public String verConsulta(@PathVariable Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            Consulta consulta = consultaService.buscarPorId(id);
            model.addAttribute("consulta", consulta);
            model.addAttribute("editable", consultaService.estaEnVentanaEdicion(consulta));
            return "vet/ver-consulta";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/vet/dashboard";
        }
    }

    @GetMapping("/consultas/{id}/editar")
    public String mostrarEdicionConsulta(@PathVariable Integer id,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        try {
            Consulta consulta = consultaService.buscarPorId(id);
            if (!consultaService.estaEnVentanaEdicion(consulta)) {
                redirectAttributes.addFlashAttribute("errorForm",
                        "No es posible editar esta consulta: han transcurrido más de 60 minutos.");
                return "redirect:/vet/consultas/" + id;
            }
            model.addAttribute("consulta", consulta);
            return "vet/editar-consulta";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/vet/dashboard";
        }
    }

    @PostMapping("/consultas/{id}/editar")
    public String editarConsulta(@PathVariable Integer id,
                                  @RequestParam String diagnostico,
                                  @RequestParam(required = false) String tratamiento,
                                  @RequestParam(required = false) String observaciones,
                                  @RequestParam(required = false) BigDecimal temperatura,
                                  @RequestParam(required = false) BigDecimal peso,
                                  @RequestParam(required = false) Integer frecuenciaCardiaca,
                                  RedirectAttributes redirectAttributes) {
        try {
            consultaService.actualizarConsulta(id, diagnostico, tratamiento,
                    observaciones, temperatura, peso, frecuenciaCardiaca);
            redirectAttributes.addFlashAttribute("mensajeExito", "Consulta actualizada correctamente.");
        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error al editar consulta_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/vet/consultas/" + id;
    }

    @GetMapping("/consultas/{id}/pdf/descargar")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        try {
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

    @PostMapping("/consultas/{id}/pdf/enviar")
    public String enviarPdfPorCorreo(@PathVariable Integer id,
                                      RedirectAttributes redirectAttributes) {
        try {
            Consulta consulta = consultaService.buscarPorId(id);
            byte[] pdf = consultaPdfService.generarPdf(id);
            Usuario propietario = consulta.getMascota().getPropietario();
            emailService.enviarConsultaPdf(
                    propietario.getEmail(),
                    propietario.getNombres() + " " + propietario.getApellidos(),
                    pdf, id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "PDF enviado exitosamente a " + propietario.getEmail()
                    + ". Indíquele al cliente que revise su carpeta de Spam si no lo visualiza en unos minutos.");
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", "Consulta no encontrada.");
        } catch (RuntimeException ex) {
            log.error("Error al enviar PDF consulta_id={}", id, ex);
            redirectAttributes.addFlashAttribute("errorForm",
                    "No se pudo enviar el correo. Verifique la configuración SMTP.");
        }
        return "redirect:/vet/consultas/" + id;
    }

    // ── Directorio de mascotas (solo lectura) ────────────────────────────────

    @GetMapping("/mascotas")
    public String listarMascotas(@RequestParam(value = "keyword", required = false) String keyword,
                                  Model model) {
        List<Mascota> mascotas = (keyword != null && !keyword.isBlank())
                ? mascotaService.listarTodas(keyword)
                : mascotaService.listarTodas();
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("keyword",  keyword != null ? keyword : "");
        return "vet/mascotas";
    }

    // ── Edición de mascota (datos físicos) ───────────────────────────────────

    @GetMapping("/mascotas/{id}/editar")
    public String mostrarFormEditarMascota(@PathVariable Integer id,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("mascota", mascotaService.buscarPorId(id));
            model.addAttribute("sexos",   Sexo.values());
            return "vet/editar-mascota";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/vet/dashboard";
        }
    }

    @PostMapping("/mascotas/{id}/editar")
    public String editarMascota(@PathVariable Integer id,
                                 @RequestParam String nombre,
                                 @RequestParam String especie,
                                 @RequestParam(required = false) String raza,
                                 @RequestParam(required = false) String color,
                                 @RequestParam(value = "fechaNacimiento", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                 @RequestParam Sexo sexo,
                                 @RequestParam(required = false) BigDecimal peso,
                                 @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fecha, sexo, peso);
            mascotaService.actualizarMascota(id, dto);

            if (archivo != null && !archivo.isEmpty()) {
                String nombreFoto = fileStorageService.guardarFotoMascota(archivo);
                mascotaService.actualizarFoto(id, nombreFoto);
            }

            log.info("Mascota id={} actualizada por veterinario", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Datos del paciente actualizados correctamente.");
            return "redirect:/vet/historial/" + id;
        } catch (Exception ex) {
            log.error("Error al actualizar mascota id={} por veterinario: {}", id, ex.getMessage());
            try { model.addAttribute("mascota", mascotaService.buscarPorId(id)); }
            catch (Exception ignored) {}
            model.addAttribute("sexos",    Sexo.values());
            model.addAttribute("errorForm", "No se pudo actualizar: " + ex.getMessage());
            return "vet/editar-mascota";
        }
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
