package com.smartvet.app.service;

import com.smartvet.app.dto.ConsultaMedicaDTO;
import com.smartvet.app.model.Consulta;

import java.math.BigDecimal;
import java.util.List;

public interface ConsultaService {

    Consulta registrarConsulta(ConsultaMedicaDTO dto);

    Consulta buscarPorId(Integer id);

    Consulta buscarPorCita(Integer idCita);

    List<Consulta> listarPorMascota(Integer idMascota);

    Consulta actualizarConsulta(Integer idConsulta,
                                String diagnostico,
                                String tratamiento,
                                String observaciones,
                                BigDecimal temperatura,
                                BigDecimal peso,
                                Integer frecuenciaCardiaca);

    boolean estaEnVentanaEdicion(Consulta consulta);
}
