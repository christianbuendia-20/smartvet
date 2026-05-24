package com.smartvet.app.repository;

import com.smartvet.app.model.DetalleReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleRecetaRepository extends JpaRepository<DetalleReceta, Integer> {

    List<DetalleReceta> findByReceta_IdReceta(Integer idReceta);
}
