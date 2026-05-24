package com.smartvet.app.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CitaNoDisponibleException extends RuntimeException {

    public CitaNoDisponibleException(Integer idVeterinario, LocalDateTime fechaHora) {
        super(String.format(
                "El veterinario (id=%d) ya tiene una cita activa en el rango de 30 minutos de %s",
                idVeterinario,
                fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
    }
}
