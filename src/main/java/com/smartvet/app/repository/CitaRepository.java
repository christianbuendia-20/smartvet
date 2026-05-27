package com.smartvet.app.repository;

import com.smartvet.app.model.Cita;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByEstado(EstadoCita estado);

    List<Cita> findByEstadoAndFechaHoraBetween(EstadoCita estado, LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findByMascota_IdMascota(Integer idMascota);

    List<Cita> findByVeterinario_IdVeterinario(Integer idVeterinario);

    List<Cita> findByVeterinario_IdVeterinarioAndFechaHoraBetween(Integer idVeterinario, LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findByMascota_IdMascotaOrderByFechaHoraDesc(Integer idMascota);

    @Query("SELECT c FROM Cita c " +
           "JOIN FETCH c.mascota " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "WHERE c.fechaHora BETWEEN :inicio AND :fin " +
           "ORDER BY c.fechaHora ASC")
    List<Cita> findCitasConDetallePorFecha(@Param("inicio") LocalDateTime inicio,
                                            @Param("fin") LocalDateTime fin);

    @Query("SELECT c FROM Cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.propietario " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "WHERE m.propietario.idUsuario = :idPropietario " +
           "ORDER BY c.fechaHora DESC")
    List<Cita> findCitasByPropietarioConDetalle(@Param("idPropietario") Integer idPropietario);

    @Query("SELECT c FROM Cita c " +
           "JOIN FETCH c.mascota " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "WHERE v.idVeterinario = :idVeterinario " +
           "ORDER BY c.fechaHora DESC")
    List<Cita> findCitasByVeterinarioConDetalle(@Param("idVeterinario") Integer idVeterinario);

    @Query("SELECT c FROM Cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.propietario " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "WHERE v.idVeterinario = :idVet " +
           "AND c.fechaHora BETWEEN :inicio AND :fin " +
           "ORDER BY c.fechaHora ASC")
    List<Cita> findCitasDeHoyByVeterinario(@Param("idVet") Integer idVet,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fin") LocalDateTime fin);

    @Query("SELECT c FROM Cita c WHERE c.veterinario.idVeterinario = :idVet " +
           "AND c.fechaHora BETWEEN :inicio AND :fin " +
           "AND c.estado IN :estadosActivos")
    List<Cita> findCitasActivasByVeterinarioAndRangoFecha(
            @Param("idVet") Integer idVet,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadosActivos") Collection<EstadoCita> estadosActivos);

    @Query("SELECT c FROM Cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.propietario " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "ORDER BY c.fechaHora DESC")
    List<Cita> findAllWithDetalle();

    @Query("SELECT c FROM Cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.propietario " +
           "JOIN FETCH c.veterinario v " +
           "JOIN FETCH v.usuario " +
           "WHERE c.estado = :estadoCita " +
           "AND NOT EXISTS (" +
           "    SELECT p FROM PagoCita p WHERE p.cita = c AND p.estado = :estadoPago" +
           ") " +
           "ORDER BY c.fechaHora DESC")
    List<Cita> findCitasCompletadasSinPago(@Param("estadoCita") EstadoCita estadoCita,
                                            @Param("estadoPago") EstadoPago estadoPago);
}
