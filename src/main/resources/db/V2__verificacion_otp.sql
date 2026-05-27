-- ==============================================
-- TABLA: verificacion_otp
-- Propósito: Almacena el código OTP de 6 dígitos generado durante el registro.
--            El usuario debe verificar su email antes de poder iniciar sesión.
--            El registro se elimina automáticamente tras la verificación exitosa.
-- Ejecutar manualmente en la BD Smartvet (ddl-auto=none).
-- ==============================================

CREATE TABLE verificacion_otp (
    id_otp          INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario      INT NOT NULL UNIQUE,
    codigo          VARCHAR(6) NOT NULL,
    fecha_creacion  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario) ON DELETE CASCADE
);
