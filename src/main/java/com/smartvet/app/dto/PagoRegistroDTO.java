package com.smartvet.app.dto;

import com.smartvet.app.model.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagoRegistroDTO(

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal monto,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        String referencia,

        String observaciones
) {}
