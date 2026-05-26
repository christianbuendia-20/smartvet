package com.smartvet.app.dto;

public record PerfilUpdateDTO(
        String nombres,
        String apellidos,
        String email,
        String telefono,
        String dni,
        String direccion,
        String referencia
) {}
