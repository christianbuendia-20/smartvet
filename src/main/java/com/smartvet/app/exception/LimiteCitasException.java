package com.smartvet.app.exception;

public class LimiteCitasException extends RuntimeException {

    public LimiteCitasException(String nombrePropietario) {
        super("El cliente " + nombrePropietario + " ya tiene 3 citas activas (PROGRAMADA o CONFIRMADA). "
              + "Debe cancelar o finalizar una cita existente antes de registrar una nueva.");
    }
}
