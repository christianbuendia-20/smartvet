package com.smartvet.app.service.impl;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.smartvet.app.dto.PagoRegistroDTO;
import com.smartvet.app.dto.PreferenciaMPResponseDTO;
import com.smartvet.app.model.EstadoPago;
import com.smartvet.app.model.MetodoPago;
import com.smartvet.app.model.PagoCita;
import com.smartvet.app.service.MercadoPagoService;
import com.smartvet.app.service.PagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final String     ADMIN_SUFFIX    = "/admin/pagos/mp";
    private static final BigDecimal TARIFA_CONSULTA = new BigDecimal("60.00");

    private final String backUrlBase;
    private final PagoService pagoService;

    public MercadoPagoServiceImpl(
            @Value("${mercadopago.back-url-base}") String backUrlBase,
            PagoService pagoService) {
        this.backUrlBase = backUrlBase;
        this.pagoService = pagoService;
    }

    @Override
    public PreferenciaMPResponseDTO crearPreferencia(Integer idCita, BigDecimal monto)
            throws MPException, MPApiException {
        return crearPreferencia(idCita, monto, ADMIN_SUFFIX);
    }

    @Override
    public PreferenciaMPResponseDTO crearPreferencia(Integer idCita, BigDecimal monto, String backUrlSuffix)
            throws MPException, MPApiException {

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(idCita.toString())
                .title("Consulta Veterinaria - Santa Victoria")
                .description("Servicio veterinario SmartVet")
                .quantity(1)
                .unitPrice(TARIFA_CONSULTA)
                .currencyId("PEN")
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(backUrlBase + backUrlSuffix + "/exito")
                .failure(backUrlBase + backUrlSuffix + "/fallo")
                .pending(backUrlBase + backUrlSuffix + "/pendiente")
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(idCita.toString())
                .statementDescriptor("SmartVet Cita #" + idCita)
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(preferenceRequest);

        log.info("Preferencia MP creada: preference_id={}, cita_id={}, monto={}, suffix={}",
                preference.getId(), idCita, monto, backUrlSuffix);

        return new PreferenciaMPResponseDTO(preference.getId(), preference.getSandboxInitPoint());
    }

    @Override
    @Transactional
    public void registrarPagoDesdeMP(String collectionId, Integer idCita)
            throws MPException, MPApiException {

        List<PagoCita> existentes = pagoService.listarPorCita(idCita);
        boolean yaRegistrado = existentes.stream()
                .anyMatch(p -> p.getEstado() == EstadoPago.COMPLETADO);
        if (yaRegistrado) {
            log.info("Callback duplicado ignorado: cita_id={}, collection_id={}", idCita, collectionId);
            return;
        }

        PaymentClient paymentClient = new PaymentClient();
        Payment payment = paymentClient.get(Long.parseLong(collectionId));

        MetodoPago metodo = switch (payment.getPaymentTypeId()) {
            case "credit_card", "debit_card", "prepaid_card" -> MetodoPago.TARJETA;
            case "account_money", "digital_currency", "digital_wallet" -> MetodoPago.TRANSFERENCIA;
            default -> MetodoPago.OTRO;
        };

        BigDecimal monto = payment.getTransactionAmount() != null
                ? payment.getTransactionAmount()
                : BigDecimal.ZERO;

        PagoRegistroDTO dto = new PagoRegistroDTO(
                monto,
                metodo,
                "MP-" + collectionId,
                "Pago procesado vía Mercado Pago (Sandbox)"
        );

        pagoService.registrarPago(idCita, dto);

        log.info("Pago MP registrado: cita_id={}, collection_id={}, monto={}, metodo={}",
                idCita, collectionId, monto, metodo);
    }
}
