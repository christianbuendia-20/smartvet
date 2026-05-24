package com.smartvet.app.repository;

import com.smartvet.app.model.Mascota;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
