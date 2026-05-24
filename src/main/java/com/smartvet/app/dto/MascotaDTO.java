package com.smartvet.app.dto;

import com.smartvet.app.model.Sexo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MascotaDTO(

        @NotBlank(message = "El nombre de la mascota es obligatorio")
        String nombre,

        @NotBlank(message = "La especie es obligatoria")
        String especie,

        String raza,

        String color,

        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento,

        @NotNull(message = "El sexo es obligatorio")
        Sexo sexo,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor a 0")
        BigDecimal peso
) {}
