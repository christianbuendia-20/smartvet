package com.smartvet.app.controller;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.smartvet.app.dto.PreferenciaMPRequestDTO;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.EstadoPago;
import com.smartvet.app.model.MetodoPago;
import com.smartvet.app.model.PagoCita;
import com.smartvet.app.repository.PagoCitaRepository;
import com.smartvet.app.security.SmartVetUserDetails;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.MercadoPagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/cliente/pagos")
public class PagoMercadoPagoClienteController {

    private static final String BASE_URL    = "http://localhost:8080/cliente/pagos";
    private static final String REDIR_PAGOS = "redirect:/cliente/mis-pagos";

    private final MercadoPagoService mercadoPagoService;
    private final CitaService        citaService;
    private final PagoCitaRepository pagoCitaRepository;

    public PagoMercadoPagoClienteController(
            MercadoPagoService mercadoPagoService,
            CitaService citaService,
            PagoCitaRepository pagoCitaRepository,
            @Value("${mercadopago.access-token}") String accessToken) {
        this.mercadoPagoService = mercadoPagoService;
        this.citaService        = citaService;
        this.pagoCitaRepository = pagoCitaRepository;
        MercadoPagoConfig.setAccessToken(accessToken);
        System.out.println("[MP] SDK inicializado con token: " + accessToken.substring(0, 12) + "...");
    }

    // ── Crear preferencia ─────────────────────────────────────────────────────
    // Ruta: POST /cliente/pagos/mp/crear-preferencia  (el /mp es del @PostMapping)

    @PostMapping("/mp/crear-preferencia")
    @ResponseBody
    public ResponseEntity<?> crearPreferencia(@RequestBody PreferenciaMPRequestDTO request) {

        Integer idUsuario = principal().getIdUsuario();
        System.out.println("[MP] POST crear-preferencia | usuario_id=" + idUsuario
                + " | cita_id=" + request.idCita());

        try {
            Cita cita = citaService.buscarPorId(request.idCita());
            if (!cita.getMascota().getPropietario().getIdUsuario().equals(idUsuario)) {
                log.warn("Cliente usuario_id={} intentó pagar cita_id={} que no le pertenece",
                        idUsuario, request.idCita());
                return ResponseEntity.status(403)
                        .body("No estás autorizado para pagar esta cita.");
            }

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(request.idCita().toString())
                    .title("Consulta Veterinaria - Santa Victoria")
                    .description("Servicio veterinario SmartVet")
                    .quantity(1)
                    .unitPrice(new BigDecimal("60.00"))
                    .currencyId("PEN")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(BASE_URL + "/exito")
                    .failure(BASE_URL + "/fallo")
                    .pending(BASE_URL + "/pendiente")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .externalReference(request.idCita().toString())
                    .build();

            System.out.println("[MP] backUrl.success = " + BASE_URL + "/exito"
                    + " | externalReference=" + request.idCita());

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            String urlCheckout = preference.getSandboxInitPoint(); // usar getInitPoint() en producción
            System.out.println("[MP] Preferencia creada OK | preference_id=" + preference.getId()
                    + " | url=" + urlCheckout);
            log.info("Preferencia MP creada: preference_id={}, cita_id={}", preference.getId(), request.idCita());

            return ResponseEntity.ok(Map.of("url", urlCheckout));

        } catch (RecursoNoEncontradoException ex) {
            System.out.println("[MP] ERROR: cita no encontrada -> " + ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (MPApiException ex) {
            String content = ex.getApiResponse() != null
                    ? ex.getApiResponse().getContent() : "(sin respuesta)";
            System.out.println("[MP] ERROR API MP: status="
                    + (ex.getApiResponse() != null ? ex.getApiResponse().getStatusCode() : -1)
                    + " | body=" + content);
            log.error("Error API MP: status={}, body={}",
                    ex.getApiResponse() != null ? ex.getApiResponse().getStatusCode() : -1, content);
            return ResponseEntity.badRequest().body("Error de Mercado Pago: " + content);

        } catch (MPException ex) {
            System.out.println("[MP] ERROR SDK MP: " + ex.getMessage());
            log.error("Error SDK MP: {}", ex.getMessage());
            return ResponseEntity.internalServerError().body("Error al conectar con Mercado Pago.");
        }
    }

    // ── Endpoint dedicado de procesamiento del pago exitoso ───────────────────
    // Ruta: GET /cliente/pagos/exito
    // MP redirige aquí con: ?status=approved&external_reference=48&collection_id=...

    @GetMapping("/exito")
    public String procesarExito(
            @RequestParam(name = "collection_id",      required = false) String collectionId,
            @RequestParam(name = "collection_status",  required = false) String collectionStatus,
            @RequestParam(name = "status",             required = false) String status,
            @RequestParam(name = "external_reference", required = false) String externalReference,
            @RequestParam(name = "preference_id",      required = false) String preferenceId,
            RedirectAttributes redirectAttributes) {

        System.out.println("=== RETORNO DE MERCADO PAGO ===");
        System.out.println("collection_id:      " + collectionId);
        System.out.println("collection_status:  " + collectionStatus);
        System.out.println("status:             " + status);
        System.out.println("external_reference: " + externalReference);
        System.out.println("preference_id:      " + preferenceId);
        System.out.println("================================");

        String estadoEfectivo = (collectionStatus != null) ? collectionStatus : status;

        if (!"approved".equals(estadoEfectivo)) {
            System.out.println("[MP] Pago no aprobado: estado=" + estadoEfectivo);
            redirectAttributes.addFlashAttribute("errorForm",
                    "El pago no fue aprobado (estado: " + estadoEfectivo
                    + "). Intenta de nuevo o usa otro método de pago.");
            return REDIR_PAGOS;
        }

        if (externalReference == null || externalReference.isBlank()) {
            System.out.println("[MP] ERROR CRÍTICO: external_reference llegó NULO.");
            System.out.println("[MP] Verificar que @PostMapping incluya .externalReference(citaId)");
            redirectAttributes.addFlashAttribute("errorForm",
                    "Pago aprobado pero falta la referencia de la cita. "
                    + "Referencia MP: " + collectionId + ". Contacta a la clínica.");
            return REDIR_PAGOS;
        }

        try {
            Integer idCita = Integer.parseInt(externalReference);
            System.out.println("[MP] Procesando pago para cita_id=" + idCita);

            /* Idempotencia: no duplicar si ya existe un COMPLETADO */
            boolean yaExiste = pagoCitaRepository.findByCita_IdCita(idCita).stream()
                    .anyMatch(p -> p.getEstado() == EstadoPago.COMPLETADO);

            if (yaExiste) {
                System.out.println("[MP] Pago duplicado ignorado (ya existe COMPLETADO): cita_id=" + idCita);
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "El pago de esta cita ya había sido registrado anteriormente.");
                return REDIR_PAGOS;
            }

            Cita cita = citaService.buscarPorId(idCita);
            System.out.println("[MP] Cita encontrada: id=" + cita.getIdCita()
                    + " | mascota=" + cita.getMascota().getNombre()
                    + " | estado=" + cita.getEstado());

            PagoCita pagoCita = new PagoCita();
            pagoCita.setCita(cita);
            pagoCita.setMonto(new BigDecimal("60.00"));
            pagoCita.setMetodoPago(MetodoPago.TRANSFERENCIA);
            pagoCita.setEstado(EstadoPago.COMPLETADO);
            pagoCita.setReferencia("MP-" + collectionId);
            pagoCita.setObservaciones("Pago procesado vía Mercado Pago Checkout Pro");

            PagoCita guardado = pagoCitaRepository.save(pagoCita);

            System.out.println("[MP] ¡PagoCita insertado en BD! idPago=" + guardado.getIdPago()
                    + " | cita_id=" + idCita + " | referencia=MP-" + collectionId);
            log.info("PagoCita creado: idPago={}, cita_id={}, referencia=MP-{}",
                    guardado.getIdPago(), idCita, collectionId);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Pago realizado correctamente. Referencia: MP-" + collectionId
                    + ". Tu historial ha sido actualizado.");

        } catch (NumberFormatException ex) {
            System.out.println("[MP] ERROR: external_reference no es número: '" + externalReference + "'");
            log.error("external_reference inválido: '{}'", externalReference);
            redirectAttributes.addFlashAttribute("errorForm",
                    "El pago fue aprobado pero no se pudo identificar la cita. "
                    + "Referencia MP: " + collectionId + ". Contacta a la clínica.");

        } catch (RecursoNoEncontradoException ex) {
            System.out.println("[MP] ERROR: cita no encontrada: " + ex.getMessage());
            log.error("Cita no encontrada al registrar pago MP: cita_id={}", externalReference);
            redirectAttributes.addFlashAttribute("errorForm",
                    "El pago fue aprobado pero la cita no existe. "
                    + "Referencia MP: " + collectionId + ". Contacta a la clínica.");

        } catch (Exception ex) {
            System.out.println("[MP] ERROR inesperado al guardar PagoCita: " + ex.getMessage());
            log.error("Error inesperado al registrar pago MP: collection_id={}", collectionId, ex);
            redirectAttributes.addFlashAttribute("errorForm",
                    "Pago aprobado pero ocurrió un error al registrarlo. "
                    + "Referencia MP: " + collectionId + ". Contacta a la clínica.");
        }

        return REDIR_PAGOS;
    }

    // ── Fallo ─────────────────────────────────────────────────────────────────

    @GetMapping("/fallo")
    public String procesarFallo(
            @RequestParam(name = "collection_id",      required = false) String collectionId,
            @RequestParam(name = "external_reference", required = false) String externalReference,
            RedirectAttributes redirectAttributes) {

        System.out.println("[MP] Pago fallido: cita_id=" + externalReference
                + " | collection_id=" + collectionId);
        log.warn("Pago MP fallido: external_reference={}, collection_id={}", externalReference, collectionId);
        redirectAttributes.addFlashAttribute("errorForm",
                "El pago fue rechazado o cancelado. Puedes intentarlo nuevamente.");
        return REDIR_PAGOS;
    }

    // ── Pendiente ─────────────────────────────────────────────────────────────

    @GetMapping("/pendiente")
    public String procesarPendiente(
            @RequestParam(name = "collection_id",      required = false) String collectionId,
            @RequestParam(name = "external_reference", required = false) String externalReference,
            RedirectAttributes redirectAttributes) {

        System.out.println("[MP] Pago pendiente: cita_id=" + externalReference
                + " | collection_id=" + collectionId);
        log.info("Pago MP pendiente: external_reference={}, collection_id={}", externalReference, collectionId);
        redirectAttributes.addFlashAttribute("errorForm",
                "Tu pago está siendo procesado. Referencia MP: " + collectionId
                + ". Se registrará automáticamente cuando sea aprobado.");
        return REDIR_PAGOS;
    }

    // ── Utilidad ──────────────────────────────────────────────────────────────

    private SmartVetUserDetails principal() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(p instanceof SmartVetUserDetails details)) {
            throw new IllegalStateException("Principal inesperado: " + p.getClass().getName());
        }
        return details;
    }
}
