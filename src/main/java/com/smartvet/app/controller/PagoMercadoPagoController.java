package com.smartvet.app.controller;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.smartvet.app.dto.PreferenciaMPRequestDTO;
import com.smartvet.app.dto.PreferenciaMPResponseDTO;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.service.MercadoPagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/pagos/mp")
public class PagoMercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    public PagoMercadoPagoController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    @PostMapping("/crear-preferencia")
    @ResponseBody
    public ResponseEntity<?> crearPreferencia(@RequestBody PreferenciaMPRequestDTO request) {
        try {
            PreferenciaMPResponseDTO respuesta =
                    mercadoPagoService.crearPreferencia(request.idCita(), request.monto());
            return ResponseEntity.ok(respuesta);
        } catch (MPApiException ex) {
            String apiContent = ex.getApiResponse() != null ? ex.getApiResponse().getContent() : "(sin respuesta)";
            log.error("Error de la API de MP al crear preferencia: status={}, mensaje={}, respuesta={}",
                    ex.getApiResponse() != null ? ex.getApiResponse().getStatusCode() : -1,
                    ex.getMessage(), apiContent);
            return ResponseEntity.badRequest()
                    .body("Error de Mercado Pago: " + apiContent);
        } catch (MPException ex) {
            log.error("Error de SDK de MP al crear preferencia: {}", ex.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Error al conectar con Mercado Pago.");
        }
    }

    @GetMapping("/exito")
    public String manejarExito(
            @RequestParam(name = "collection_id",    required = false) String collectionId,
            @RequestParam(name = "collection_status", required = false) String collectionStatus,
            @RequestParam(name = "external_reference", required = false) String externalReference,
            RedirectAttributes redirectAttributes) {

        if (!"approved".equals(collectionStatus)) {
            log.warn("Callback /exito con estado inesperado: collection_status={}, collection_id={}",
                    collectionStatus, collectionId);
            redirectAttributes.addFlashAttribute("errorForm",
                    "El pago no fue aprobado. Estado recibido: " + collectionStatus);
            return "redirect:/admin/pagos";
        }

        try {
            Integer idCita = Integer.parseInt(externalReference);
            mercadoPagoService.registrarPagoDesdeMP(collectionId, idCita);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Pago con Mercado Pago registrado correctamente para la Cita #" + idCita
                    + ". Referencia: MP-" + collectionId);
        } catch (NumberFormatException ex) {
            log.error("external_reference inválido: '{}'", externalReference);
            redirectAttributes.addFlashAttribute("errorForm",
                    "El pago fue procesado pero no se pudo registrar (referencia inválida). "
                    + "Referencia MP: " + collectionId);
        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error de negocio al registrar pago MP: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        } catch (MPException | MPApiException ex) {
            log.error("Error al verificar pago MP collection_id={}: {}", collectionId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm",
                    "El pago fue aprobado en Mercado Pago pero ocurrió un error al registrarlo. "
                    + "Referencia MP: " + collectionId + ". Contácta al administrador.");
        }

        return "redirect:/admin/pagos";
    }

    @GetMapping("/pendiente")
    public String manejarPendiente(
            @RequestParam(name = "external_reference", required = false) String externalReference,
            @RequestParam(name = "collection_id", required = false) String collectionId,
            RedirectAttributes redirectAttributes) {

        log.info("Pago MP pendiente: external_reference={}, collection_id={}",
                externalReference, collectionId);
        redirectAttributes.addFlashAttribute("errorForm",
                "El pago está pendiente de confirmación. Referencia MP: " + collectionId
                + ". Se registrará automáticamente cuando sea aprobado.");
        return "redirect:/admin/pagos";
    }

    @GetMapping("/fallo")
    public String manejarFallo(
            @RequestParam(name = "external_reference", required = false) String externalReference,
            @RequestParam(name = "collection_id", required = false) String collectionId,
            RedirectAttributes redirectAttributes) {

        log.warn("Pago MP fallido: external_reference={}, collection_id={}",
                externalReference, collectionId);
        redirectAttributes.addFlashAttribute("errorForm",
                "El pago fue rechazado o cancelado en Mercado Pago. Puedes intentarlo de nuevo o usar otro método de pago.");
        return "redirect:/admin/pagos";
    }
}
