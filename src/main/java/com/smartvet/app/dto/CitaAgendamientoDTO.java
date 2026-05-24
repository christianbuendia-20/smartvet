package com.smartvet.app.dto;

import com.smartvet.app.model.TipoCita;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CitaAgendamientoDTO(

        @NotNull(message = "Debe seleccionar una mascota")
        Integer idMascota,

        @NotNull(message = "Debe seleccionar un veterinario")
        Integer idVeterinario,

        @NotNull(message = "El tipo de cita es obligatorio")
        TipoCita tipoCita,

        @NotNull(message = "La fecha y hora son obligatorias")
        @Future(message = "La cita debe programarse en una fecha futura")
        LocalDateTime fechaHora,

        @NotBlank(message = "El motivo de la cita es obligatorio")
        String motivo
) {}
