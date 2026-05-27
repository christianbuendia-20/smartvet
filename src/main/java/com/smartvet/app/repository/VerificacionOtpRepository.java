package com.smartvet.app.repository;

import com.smartvet.app.model.VerificacionOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionOtpRepository extends JpaRepository<VerificacionOtp, Integer> {

    Optional<VerificacionOtp> findByUsuario_IdUsuario(Integer idUsuario);

    void deleteByUsuario_IdUsuario(Integer idUsuario);
}
