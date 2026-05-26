package com.smartvet.app.repository;

import com.smartvet.app.model.DireccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DireccionUsuarioRepository extends JpaRepository<DireccionUsuario, Integer> {

    Optional<DireccionUsuario> findFirstByUsuario_IdUsuario(Integer idUsuario);
}
