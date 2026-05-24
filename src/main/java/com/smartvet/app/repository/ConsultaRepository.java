package com.smartvet.app.repository;

import com.smartvet.app.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Integer> {

    Optional<Consulta> findByCita_IdCita(Integer idCita);

    List<Consulta> findByVeterinario_IdVeterinario(Integer idVeterinario);

    List<Consulta> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT c FROM Consulta c " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "WHERE c.mascota.idMascota = :idMascota " +
           "ORDER BY c.fechaHora DESC")
    List<Consulta> findByMascotaConDetalle(@Param("idMascota") Integer idMascota);
}
