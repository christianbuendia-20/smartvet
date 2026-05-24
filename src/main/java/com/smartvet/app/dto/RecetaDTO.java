package com.smartvet.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecetaDTO(

        @NotBlank(message = "Las instrucciones generales de la receta son obligatorias")
        String instrucciones,

        @Min(value = 1, message = "La vigencia mínima es 1 día")
        Integer vigenciaDias,

        @NotEmpty(message = "La receta debe incluir al menos un medicamento")
        @Valid
        List<DetalleRecetaDTO> detalles
) {}
