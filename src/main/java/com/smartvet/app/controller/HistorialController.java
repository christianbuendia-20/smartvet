package com.smartvet.app.controller;

import com.smartvet.app.dto.HistoriaClinicaUpdateDTO;
import com.smartvet.app.exception.AccesoDenegadoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.HistoriaClinica;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.MascotaService;
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

@Slf4j
@Controller
@RequestMapping("/mascota")
public class HistorialController {

    private final MascotaService mascotaService;

    public HistorialController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    private SmartVetUserDetails principal() {
        return (SmartVetUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
    }

    private String historialUrl(Integer idMascota) {
        String rol = principal().getRolNombre();
        return switch (rol) {
            case "veterinario" -> "/vet/historial/" + idMascota;
            case "cliente"     -> "/cliente/mascotas/" + idMascota + "/historial";
            default            -> "/";
        };
    }

    @GetMapping("/historial/editar/{idMascota}")
    public String mostrarFormEditar(@PathVariable Integer idMascota,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            Mascota mascota          = mascotaService.buscarPorId(idMascota);
            HistoriaClinica historia = mascotaService.verHistorialClinico(idMascota);
            model.addAttribute("mascota",    mascota);
            model.addAttribute("historia",   historia);
            model.addAttribute("returnUrl",  historialUrl(idMascota));
            return "editar-historial";
        } catch (AccesoDenegadoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:" + historialUrl(idMascota);
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/historial/editar/{idMascota}")
    public String guardarHistorial(@PathVariable Integer idMascota,
                                    @RequestParam(required = false) String alergias,
                                    @RequestParam(required = false) String enfermedadesCronicas,
                                    @RequestParam(required = false) String notasGenerales,
                                    RedirectAttributes redirectAttributes) {
        try {
            HistoriaClinicaUpdateDTO dto = new HistoriaClinicaUpdateDTO(
                    alergias, enfermedadesCronicas, notasGenerales);
            mascotaService.actualizarHistoriaClinica(idMascota, dto);
            log.info("Historia clínica editada: mascota_id={}, usuario_id={}",
                    idMascota, principal().getIdUsuario());
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Historia clínica actualizada correctamente.");
        } catch (AccesoDenegadoException | RecursoNoEncontradoException ex) {
            log.warn("Error al actualizar historia clínica mascota_id={}: {}", idMascota, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorAcceso", ex.getMessage());
        }
        return "redirect:" + historialUrl(idMascota);
    }
}
