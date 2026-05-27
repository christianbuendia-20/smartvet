package com.smartvet.app.service;

public interface EmailService {

    void enviarCodigoVerificacion(String destinatario, String nombreCompleto, String codigo);

    void enviarConsultaPdf(String destinatario, String nombreCompleto,
                            byte[] pdfBytes, Integer idConsulta);

    void enviarRecuperacionContrasena(String destinatario, String nombreCompleto, String codigo);
}
