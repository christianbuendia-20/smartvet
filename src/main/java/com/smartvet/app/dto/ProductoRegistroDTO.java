package com.smartvet.app.dto;

import com.smartvet.app.model.TipoProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoRegistroDTO(

        @NotBlank(message = "El nombre del producto es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El tipo de producto es obligatorio")
        TipoProducto tipoProducto,

        @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
        BigDecimal precioCompra,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
        BigDecimal precioVenta,

        @Min(value = 0, message = "El stock inicial no puede ser negativo")
        Integer stockActual,

        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,

        String unidadMedida,

        Boolean requiereReceta
) {}
