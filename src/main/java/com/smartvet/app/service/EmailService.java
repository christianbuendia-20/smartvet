package com.smartvet.app.service;

public interface EmailService {

    /**
     * Envía un correo real al destinatario con el código OTP de verificación.
     * Lanza RuntimeException si el envío falla (SMTP no configurado, red, etc.).
     */
    void enviarCodigoVerificacion(String destinatario, String nombreCompleto, String codigo);
}
