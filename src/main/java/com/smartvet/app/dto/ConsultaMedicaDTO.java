package com.smartvet.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConsultaMedicaDTO(

        @NotNull(message = "El ID de la cita es obligatorio")
        Integer idCita,

        @NotNull(message = "La fecha y hora de la consulta son obligatorias")
        LocalDateTime fechaHora,

        @DecimalMin(value = "0.1", message = "La temperatura debe ser mayor a 0")
        BigDecimal temperatura,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
        BigDecimal peso,

        @Min(value = 1, message = "La frecuencia cardíaca debe ser mayor a 0")
        Integer frecuenciaCardiaca,

        @NotBlank(message = "El diagnóstico es obligatorio")
        String diagnostico,

        String tratamiento,

        String observaciones,

        // Null si la consulta no genera receta
        @Valid
        RecetaDTO receta
) {}
