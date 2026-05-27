package com.smartvet.app.service.impl;

import com.smartvet.app.model.Usuario;
import com.smartvet.app.model.VerificacionOtp;
import com.smartvet.app.repository.VerificacionOtpRepository;
import com.smartvet.app.service.EmailService;
import com.smartvet.app.service.VerificacionOtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
public class VerificacionOtpServiceImpl implements VerificacionOtpService {

    private final VerificacionOtpRepository otpRepository;
    private final EmailService              emailService;

    public VerificacionOtpServiceImpl(VerificacionOtpRepository otpRepository,
                                       EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService  = emailService;
    }

    @Override
    @Transactional
    public void crearYEnviar(Usuario usuario) {
        otpRepository.deleteByUsuario_IdUsuario(usuario.getIdUsuario());

        String codigo = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        VerificacionOtp otp = new VerificacionOtp();
        otp.setUsuario(usuario);
        otp.setCodigo(codigo);
        otpRepository.save(otp);

        emailService.enviarCodigoVerificacion(
                usuario.getEmail(),
                usuario.getNombres() + " " + usuario.getApellidos(),
                codigo);

        log.info("OTP creado y correo enviado para usuario_id={}", usuario.getIdUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificar(Integer idUsuario, String codigoIngresado) {
        return otpRepository.findByUsuario_IdUsuario(idUsuario)
                .map(otp -> otp.getCodigo().equals(codigoIngresado.trim()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void eliminar(Integer idUsuario) {
        otpRepository.deleteByUsuario_IdUsuario(idUsuario);
        log.info("OTP eliminado para usuario_id={}", idUsuario);
    }
}
