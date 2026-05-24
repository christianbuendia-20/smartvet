package com.smartvet.app.exception;

public class MascotaNoEncontradaException extends RecursoNoEncontradoException {

    public MascotaNoEncontradaException(Integer id) {
        super("Mascota con id=" + id + " no encontrada");
    }

    public MascotaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
