package com.smartvet.app.repository;

import com.smartvet.app.model.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Integer> {

    List<Receta> findByConsulta_IdConsulta(Integer idConsulta);

    List<Receta> findByConsulta_Mascota_IdMascota(Integer idMascota);

    List<Receta> findByFechaEmisionBetween(LocalDateTime inicio, LocalDateTime fin);
}
