package com.smartvet.app.service.impl;

import com.smartvet.app.dto.CitaAgendamientoDTO;
import com.smartvet.app.exception.CitaNoDisponibleException;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.repository.CitaRepository;
import com.smartvet.app.repository.MascotaRepository;
import com.smartvet.app.repository.VeterinarioRepository;
import com.smartvet.app.security.SecurityUtils;
import com.smartvet.app.service.CitaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class CitaServiceImpl implements CitaService {

    private static final Set<EstadoCita> ESTADOS_ACTIVOS = Set.of(
            EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA, EstadoCita.EN_CURSO);

    private static final Set<EstadoCita> ESTADOS_TERMINALES = Set.of(
            EstadoCita.COMPLETADA, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO);

    private final CitaRepository        citaRepository;
    private final MascotaRepository     mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final SecurityUtils         securityUtils;

    public CitaServiceImpl(CitaRepository citaRepository,
                            MascotaRepository mascotaRepository,
                            VeterinarioRepository veterinarioRepository,
                            SecurityUtils securityUtils) {
        this.citaRepository        = citaRepository;
        this.mascotaRepository     = mascotaRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.securityUtils         = securityUtils;
    }

    @Override
    @Transactional
    public Cita programarCita(CitaAgendamientoDTO dto) {
        Mascota mascota = mascotaRepository.findById(dto.idMascota())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Mascota con id=" + dto.idMascota() + " no encontrada"));

        securityUtils.verificarPropiedadMascota(mascota);

        Veterinario veterinario = veterinarioRepository.findById(dto.idVeterinario())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Veterinario con id=" + dto.idVeterinario() + " no encontrado"));

        validarDisponibilidadVeterinario(veterinario.getIdVeterinario(), dto.fechaHora());

        Cita cita = new Cita();
        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
        cita.setTipoCita(dto.tipoCita());
        cita.setFechaHora(dto.fechaHora());
        cita.setEstado(EstadoCita.PROGRAMADA);
        cita.setMotivo(dto.motivo());

        Cita guardada = citaRepository.save(cita);
        log.info("Cita programada: id={}, mascota='{}', veterinario_id={}, fecha={}",
                guardada.getIdCita(), mascota.getNombre(),
                veterinario.getIdVeterinario(), dto.fechaHora());
        return guardada;
    }

    @Override
    public Cita buscarPorId(Integer id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita con id=" + id + " no encontrada"));
    }

    @Override
    public List<Cita> listarPorVeterinario(Integer idVeterinario) {
        return citaRepository.findByVeterinario_IdVeterinario(idVeterinario);
    }

    @Override
    public List<Cita> listarPorMascota(Integer idMascota) {
        return citaRepository.findByMascota_IdMascotaOrderByFechaHoraDesc(idMascota);
    }

    @Override
    public List<Cita> listarPorEstado(EstadoCita estado) {
        return citaRepository.findByEstado(estado);
    }

    @Override
    @Transactional
    public Cita cambiarEstado(Integer idCita, EstadoCita nuevoEstado) {
        Cita cita = buscarPorId(idCita);

        if (ESTADOS_TERMINALES.contains(cita.getEstado())) {
            throw new EstadoInvalidoException(String.format(
                    "La cita id=%d está en estado %s y no puede modificarse", idCita, cita.getEstado()));
        }

        EstadoCita estadoAnterior = cita.getEstado();
        cita.setEstado(nuevoEstado);
        Cita actualizada = citaRepository.save(cita);

        log.info("Estado de cita id={} cambiado: {} → {}", idCita, estadoAnterior, nuevoEstado);
        return actualizada;
    }

    @Override
    @Transactional
    public void cancelarCita(Integer idCita) {
        Cita cita = buscarPorId(idCita);
        securityUtils.verificarPropiedadCita(cita);
        cambiarEstado(idCita, EstadoCita.CANCELADA);
        log.info("Cita cancelada: id={}", idCita);
    }

    @Override
    public List<Cita> listarPorPropietario(Integer idUsuario) {
        return citaRepository.findCitasByPropietarioConDetalle(idUsuario);
    }

    @Override
    public List<Cita> listarPorUsuarioVeterinario(Integer idUsuario) {
        Veterinario vet = veterinarioRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró perfil de veterinario para el usuario id=" + idUsuario));
        return citaRepository.findCitasByVeterinarioConDetalle(vet.getIdVeterinario());
    }

    @Override
    public List<Veterinario> listarVeterinariosParaCita() {
        return veterinarioRepository.findAllWithUsuario();
    }

    @Override
    public List<Cita> listarCitasDeHoy() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return citaRepository.findCitasConDetallePorFecha(inicio, fin);
    }

    @Override
    public List<Cita> listarCitasDeHoyPorVeterinario(Integer idUsuario) {
        Veterinario vet = veterinarioRepository.findByUsuario_IdUsuario(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró perfil de veterinario para el usuario id=" + idUsuario));
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin    = LocalDate.now().atTime(LocalTime.MAX);
        return citaRepository.findCitasDeHoyByVeterinario(vet.getIdVeterinario(), inicio, fin);
    }

    @Override
    public List<Cita> listarProximasCitasPorPropietario(Integer idUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        Set<EstadoCita> activas = Set.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA);
        return citaRepository.findCitasByPropietarioConDetalle(idUsuario).stream()
                .filter(c -> c.getFechaHora().isAfter(ahora) && activas.contains(c.getEstado()))
                .toList();
    }

    @Override
    public long contarPendientes(List<Cita> citas) {
        return citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA
                          || c.getEstado() == EstadoCita.CONFIRMADA)
                .count();
    }

    @Override
    public long contarPorEstado(List<Cita> citas, EstadoCita estado) {
        return citas.stream().filter(c -> c.getEstado() == estado).count();
    }

    @Override
    public long contarCitasHistoricas(Integer idUsuario) {
        return citaRepository.findCitasByPropietarioConDetalle(idUsuario).stream()
                .filter(c -> ESTADOS_TERMINALES.contains(c.getEstado()))
                .count();
    }

    @Override
    public List<Cita> listarAgendaPorVeterinario(Integer idUsuario) {
        return listarPorUsuarioVeterinario(idUsuario).stream()
                .sorted(java.util.Comparator.comparing(Cita::getFechaHora))
                .toList();
    }

    // ── lógica de disponibilidad ──────────────────────────────────────────────

    private void validarDisponibilidadVeterinario(Integer idVeterinario, LocalDateTime fechaHora) {
        LocalDateTime inicio = fechaHora.minusMinutes(30);
        LocalDateTime fin = fechaHora.plusMinutes(30);

        List<Cita> conflictos = citaRepository.findCitasActivasByVeterinarioAndRangoFecha(
                idVeterinario, inicio, fin, ESTADOS_ACTIVOS);

        if (!conflictos.isEmpty()) {
            log.warn("Conflicto de agenda: veterinario_id={}, fecha solicitada={}, citas en conflicto={}",
                    idVeterinario, fechaHora, conflictos.size());
            throw new CitaNoDisponibleException(idVeterinario, fechaHora);
        }
    }
}
