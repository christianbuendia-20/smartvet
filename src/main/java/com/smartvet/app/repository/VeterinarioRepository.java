package com.smartvet.app.repository;

import com.smartvet.app.model.Veterinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VeterinarioRepository extends JpaRepository<Veterinario, Integer> {

    Optional<Veterinario> findByUsuario_IdUsuario(Integer idUsuario);

    @Query("SELECT v FROM Veterinario v JOIN FETCH v.usuario u JOIN u.rol r WHERE r.nombre = 'veterinario' ORDER BY u.nombres ASC")
    List<Veterinario> findAllWithUsuario();
}
