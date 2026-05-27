package com.smartvet.app.service;

import com.smartvet.app.model.Usuario;

public interface VerificacionOtpService {

    /** Genera un OTP, lo persiste y envía el correo de verificación de cuenta. */
    void crearYEnviar(Usuario usuario);

    /** Genera un OTP, lo persiste y envía el correo de recuperación de contraseña. */
    void crearYEnviarRecuperacion(Usuario usuario);

    /** Retorna true si el código ingresado coincide con el OTP almacenado. */
    boolean verificar(Integer idUsuario, String codigoIngresado);

    /** Elimina el registro OTP una vez verificado o en caso de error. */
    void eliminar(Integer idUsuario);
}
