package com.smartvet.app.repository;

import com.smartvet.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT u FROM Usuario u WHERE " +
           "LOWER(u.nombres) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.dni LIKE CONCAT('%', :keyword, '%')")
    List<Usuario> buscarPorKeyword(@Param("keyword") String keyword);
}
