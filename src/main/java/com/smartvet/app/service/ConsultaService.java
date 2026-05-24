package com.smartvet.app.service;

import com.smartvet.app.dto.ConsultaMedicaDTO;
import com.smartvet.app.model.Consulta;

import java.util.List;

public interface ConsultaService {

    Consulta registrarConsulta(ConsultaMedicaDTO dto);

    Consulta buscarPorId(Integer id);

    Consulta buscarPorCita(Integer idCita);

    List<Consulta> listarPorMascota(Integer idMascota);
}
