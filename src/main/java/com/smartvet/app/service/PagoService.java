package com.smartvet.app.service;

import com.smartvet.app.dto.PagoRegistroDTO;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.PagoCita;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoService {

    PagoCita registrarPago(Integer idCita, PagoRegistroDTO dto);

    PagoCita buscarPorId(Integer id);

    List<PagoCita> listarPorCita(Integer idCita);

    List<Cita> listarCitasCompletadasSinPago();

    BigDecimal calcularRecaudacion(LocalDateTime inicio, LocalDateTime fin);
}
