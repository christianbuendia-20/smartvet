package com.smartvet.app.service;

import com.smartvet.app.dto.CitaAgendamientoDTO;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.Veterinario;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {

    Cita programarCita(CitaAgendamientoDTO dto);

    Cita buscarPorId(Integer id);

    List<Cita> listarPorVeterinario(Integer idVeterinario);

    List<Cita> listarPorMascota(Integer idMascota);

    List<Cita> listarPorEstado(EstadoCita estado);

    Cita cambiarEstado(Integer idCita, EstadoCita nuevoEstado);

    void cancelarCita(Integer idCita);

    List<Cita> listarCitasDeHoy();

    List<Cita> listarCitasDeManana();

    List<Cita> listarCitasDeHoyPorVeterinario(Integer idUsuario);

    List<Cita> listarPorPropietario(Integer idUsuario);

    List<Cita> listarPorUsuarioVeterinario(Integer idUsuario);

    List<Veterinario> listarVeterinariosParaCita();

    List<Cita> listarProximasCitasPorPropietario(Integer idUsuario);

    long contarPendientes(List<Cita> citas);

    long contarPorEstado(List<Cita> citas, EstadoCita estado);

    long contarCitasHistoricas(Integer idUsuario);

    List<Cita> listarAgendaPorVeterinario(Integer idUsuario);

    List<Cita> listarProximasCitasPorVeterinario(Integer idUsuario);

    Cita reasignarVeterinario(Integer idCita, Integer idVeterinario);

    List<Cita> listarTodas();

    List<Cita> listarTodas(String keyword);

    List<Cita> listarTodas(String keyword, LocalDate fecha);

    List<Cita> listarAgendaPorVeterinario(Integer idUsuario, String keyword);

    List<Cita> listarAgendaPorVeterinario(Integer idUsuario, String keyword, LocalDate fecha);

    List<Cita> listarPorPropietario(Integer idUsuario, String keyword, LocalDate fecha);

    long contarCitasActivasPorPropietario(Integer idPropietario);

    long contarCitasActivasPorMascota(Integer idMascota);

    List<Cita> listarCitasPendientesPagoPorPropietario(Integer idPropietario);
}
