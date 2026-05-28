package com.smartvet.app.repository;

import com.smartvet.app.model.Mascota;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Integer> {

    List<Mascota> findByPropietario_IdUsuario(Integer idPropietario);

    List<Mascota> findByPropietario_IdUsuarioAndActivoTrue(Integer idPropietario);

    Page<Mascota> findByPropietario_IdUsuarioAndActivoTrue(Integer idPropietario, Pageable pageable);

    List<Mascota> findByNombreContainingIgnoreCase(String nombre);

    List<Mascota> findByEspecie(String especie);

    List<Mascota> findByActivoTrue();

    @Query("SELECT m FROM Mascota m WHERE " +
           "LOWER(m.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.propietario.nombres) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.propietario.apellidos) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Mascota> buscarPorKeyword(@Param("keyword") String keyword);
}
