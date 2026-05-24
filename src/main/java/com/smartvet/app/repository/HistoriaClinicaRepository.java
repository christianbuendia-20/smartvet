package com.smartvet.app.repository;

import com.smartvet.app.model.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Integer> {

    Optional<HistoriaClinica> findByMascota_IdMascota(Integer idMascota);
}
