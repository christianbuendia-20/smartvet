package com.smartvet.app.repository;

import com.smartvet.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByDni(String dni);

    List<Usuario> findByActivoTrue();

    List<Usuario> findByRol_Nombre(String nombreRol);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);
}
