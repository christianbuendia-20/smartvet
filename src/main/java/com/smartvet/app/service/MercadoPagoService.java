package com.smartvet.app.service;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.smartvet.app.dto.PreferenciaMPResponseDTO;

import java.math.BigDecimal;

public interface MercadoPagoService {

    /** Crea preferencia con back_urls apuntando a la ruta de administración. */
    PreferenciaMPResponseDTO crearPreferencia(Integer idCita, BigDecimal monto)
            throws MPException, MPApiException;

    /** Crea preferencia con back_urls apuntando al sufijo indicado (ej. /cliente/pagos/mp). */
    PreferenciaMPResponseDTO crearPreferencia(Integer idCita, BigDecimal monto, String backUrlSuffix)
            throws MPException, MPApiException;

    void registrarPagoDesdeMP(String collectionId, Integer idCita)
            throws MPException, MPApiException;
}
