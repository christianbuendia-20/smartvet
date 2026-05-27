package com.smartvet.app.controller;

import com.smartvet.app.dto.HistoriaClinicaUpdateDTO;
import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.HistoriaClinica;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Sexo;
import com.smartvet.app.service.ConsultaService;
import com.smartvet.app.service.MascotaService;
import com.smartvet.app.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Controller
@RequestMapping("/admin/mascotas")
public class AdminMascotaController {

    private final MascotaService  mascotaService;
    private final UsuarioService  usuarioService;
    private final ConsultaService consultaService;

    public AdminMascotaController(MascotaService mascotaService,
                                   UsuarioService usuarioService,
                                   ConsultaService consultaService) {
        this.mascotaService  = mascotaService;
        this.usuarioService  = usuarioService;
        this.consultaService = consultaService;
    }

    // ── Listado ───────────────────────────────────────────────────────────────

    @GetMapping
    public String listarMascotas(Model model) {
        model.addAttribute("mascotas", mascotaService.listarTodas());
        return "admin/mascotas";
    }

    // ── Ver historial clínico ─────────────────────────────────────────────────

    @GetMapping("/{id}/historial")
    public String verHistorial(@PathVariable Integer id,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Mascota mascota          = mascotaService.buscarPorId(id);
            HistoriaClinica historia = mascotaService.verHistorialClinico(id);
            model.addAttribute("mascota",   mascota);
            model.addAttribute("historia",  historia);
            model.addAttribute("consultas", consultaService.listarPorMascota(id));
            return "admin/historial-mascota";
        } catch (Exception ex) {
            log.warn("Admin no pudo ver historial mascota_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/mascotas";
        }
    }

    // ── Registrar nueva mascota ───────────────────────────────────────────────

    @GetMapping("/nueva")
    public String mostrarFormNueva(Model model) {
        model.addAttribute("clientes", usuarioService.listarClientes());
        model.addAttribute("sexos", Sexo.values());
        return "admin/nueva-mascota";
    }

    @PostMapping("/nueva")
    public String registrarMascota(@RequestParam Integer idPropietario,
                                    @RequestParam String nombre,
                                    @RequestParam String especie,
                                    @RequestParam(required = false) String raza,
                                    @RequestParam(required = false) String color,
                                    @RequestParam(required = false) LocalDate fechaNacimiento,
                                    @RequestParam Sexo sexo,
                                    @RequestParam(required = false) BigDecimal peso,
                                    @RequestParam(required = false) String alergias,
                                    @RequestParam(required = false) String enfermedadesCronicas,
                                    @RequestParam(required = false) String notasGenerales,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fechaNacimiento, sexo, peso);
            Mascota guardada = mascotaService.registrarMascota(dto, idPropietario);

            boolean tieneHistoria = (alergias != null && !alergias.isBlank())
                    || (enfermedadesCronicas != null && !enfermedadesCronicas.isBlank())
                    || (notasGenerales != null && !notasGenerales.isBlank());
            if (tieneHistoria) {
                mascotaService.actualizarHistoriaClinica(guardada.getIdMascota(),
                        new HistoriaClinicaUpdateDTO(alergias, enfermedadesCronicas, notasGenerales));
            }

            log.info("Admin registró mascota: id={}, nombre='{}', propietario_id={}",
                    guardada.getIdMascota(), nombre, idPropietario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Mascota \"" + nombre + "\" registrada correctamente.");
            return "redirect:/admin/mascotas";
        } catch (Exception ex) {
            log.error("Error al registrar mascota '{}': {}", nombre, ex.getMessage());
            model.addAttribute("errorForm",            ex.getMessage());
            model.addAttribute("clientes",             usuarioService.listarClientes());
            model.addAttribute("sexos",                Sexo.values());
            model.addAttribute("idPropietarioPrev",    idPropietario);
            model.addAttribute("nombrePrev",           nombre);
            model.addAttribute("especiePrev",          especie);
            model.addAttribute("razaPrev",             raza);
            model.addAttribute("colorPrev",            color);
            model.addAttribute("fechaNacimientoPrev",  fechaNacimiento);
            model.addAttribute("sexoPrev",             sexo);
            model.addAttribute("pesoPrev",             peso);
            model.addAttribute("alergiaPrev",          alergias);
            model.addAttribute("enfermedadesPrev",     enfermedadesCronicas);
            model.addAttribute("notasPrev",            notasGenerales);
            return "admin/nueva-mascota";
        }
    }

    // ── Editar mascota ────────────────────────────────────────────────────────

    @GetMapping("/editar/{id}")
    public String mostrarFormEditar(@PathVariable Integer id,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            Mascota mascota = mascotaService.buscarPorId(id);
            model.addAttribute("mascota",  mascota);
            model.addAttribute("historia", mascotaService.verHistorialClinico(id));
            model.addAttribute("sexos",    Sexo.values());
            return "admin/editar-mascota";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/mascotas";
        }
    }

    @PostMapping("/editar/{id}")
    public String editarMascota(@PathVariable Integer id,
                                 @RequestParam String nombre,
                                 @RequestParam String especie,
                                 @RequestParam(required = false) String raza,
                                 @RequestParam(required = false) String color,
                                 @RequestParam(required = false) LocalDate fechaNacimiento,
                                 @RequestParam Sexo sexo,
                                 @RequestParam(required = false) BigDecimal peso,
                                 @RequestParam(required = false) String alergias,
                                 @RequestParam(required = false) String enfermedadesCronicas,
                                 @RequestParam(required = false) String notasGenerales,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            MascotaDTO dto = new MascotaDTO(nombre, especie, raza, color, fechaNacimiento, sexo, peso);
            mascotaService.actualizarMascota(id, dto);
            mascotaService.actualizarHistoriaClinica(id,
                    new HistoriaClinicaUpdateDTO(alergias, enfermedadesCronicas, notasGenerales));
            log.info("Admin actualizó mascota: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Mascota actualizada correctamente.");
            return "redirect:/admin/mascotas";
        } catch (Exception ex) {
            log.error("Error al actualizar mascota id={}: {}", id, ex.getMessage());
            model.addAttribute("errorForm", ex.getMessage());
            try {
                model.addAttribute("mascota",  mascotaService.buscarPorId(id));
                model.addAttribute("historia", mascotaService.verHistorialClinico(id));
            } catch (Exception ignored) {}
            model.addAttribute("sexos", Sexo.values());
            return "admin/editar-mascota";
        }
    }

    // ── Dar de baja ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/baja")
    public String darDeBaja(@PathVariable Integer id,
                             RedirectAttributes redirectAttributes) {
        try {
            mascotaService.darDeBaja(id);
            log.info("Admin dio de baja mascota: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Mascota dada de baja. Ya no aparecerá en el sistema activo.");
        } catch (Exception ex) {
            log.warn("Error al dar de baja mascota id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/mascotas";
    }

    // ── Activar mascota ───────────────────────────────────────────────────────

    @PostMapping("/{id}/activar")
    public String activarMascota(@PathVariable Integer id,
                                  RedirectAttributes redirectAttributes) {
        try {
            mascotaService.activarMascota(id);
            log.info("Admin reactivó mascota: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Mascota reactivada correctamente.");
        } catch (Exception ex) {
            log.warn("Error al reactivar mascota id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/mascotas";
    }
}
