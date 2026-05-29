package com.smartvet.app.dto;

import com.smartvet.app.model.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoFilaDTO(
        Integer       idPago,
        Integer       idCita,
        LocalDateTime fechaCita,
        LocalDateTime fechaPago,
        String        nombreMascota,
        String        especieMascota,
        String        nombrePropietario,
        BigDecimal    monto,
        String        metodoPago,
        String        referencia,
        EstadoPago    estado
) {
    public boolean isPendiente() {
        return estado == EstadoPago.PENDIENTE;
    }
}
