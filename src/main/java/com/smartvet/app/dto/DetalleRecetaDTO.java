package com.smartvet.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleRecetaDTO(

        @NotNull(message = "Debe seleccionar un medicamento")
        Integer idProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1 unidad")
        Integer cantidad,

        String dosis,

        String frecuencia,

        @Min(value = 1, message = "La duración mínima es 1 día")
        Integer duracionDias,

        String instruccionesAdicionales
) {}
