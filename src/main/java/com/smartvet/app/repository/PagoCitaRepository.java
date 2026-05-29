package com.smartvet.app.repository;

import com.smartvet.app.model.EstadoPago;
import com.smartvet.app.model.MetodoPago;
import com.smartvet.app.model.PagoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoCitaRepository extends JpaRepository<PagoCita, Integer> {

    @Query("SELECT p FROM PagoCita p " +
           "JOIN FETCH p.cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.propietario " +
           "ORDER BY p.fechaPago DESC")
    List<PagoCita> findAllWithDetalle();

    List<PagoCita> findByCita_IdCita(Integer idCita);

    List<PagoCita> findByEstado(EstadoPago estado);

    List<PagoCita> findByMetodoPago(MetodoPago metodoPago);

    List<PagoCita> findByFechaPagoBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT SUM(p.monto) FROM PagoCita p WHERE p.fechaPago BETWEEN :inicio AND :fin AND p.estado = 'COMPLETADO'")
    BigDecimal sumMontoByFechaPagoBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
