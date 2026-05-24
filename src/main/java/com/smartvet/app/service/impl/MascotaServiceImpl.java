package com.smartvet.app.service.impl;

import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.exception.MascotaNoEncontradaException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.HistoriaClinica;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.repository.HistoriaClinicaRepository;
import com.smartvet.app.repository.MascotaRepository;
import com.smartvet.app.repository.UsuarioRepository;
import com.smartvet.app.security.SecurityUtils;
import com.smartvet.app.service.MascotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository      mascotaRepository;
    private final UsuarioRepository      usuarioRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final SecurityUtils          securityUtils;

    public MascotaServiceImpl(MascotaRepository mascotaRepository,
                               UsuarioRepository usuarioRepository,
                               HistoriaClinicaRepository historiaClinicaRepository,
                               SecurityUtils securityUtils) {
        this.mascotaRepository        = mascotaRepository;
        this.usuarioRepository        = usuarioRepository;
        this.historiaClinicaRepository = historiaClinicaRepository;
        this.securityUtils            = securityUtils;
    }

    @Override
    @Transactional
    public Mascota registrarMascota(MascotaDTO dto, Integer idPropietario) {
        Usuario propietario = usuarioRepository.findById(idPropietario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Propietario con id=" + idPropietario + " no encontrado"));

        Mascota mascota = construirMascota(dto, propietario);
        Mascota guardada = mascotaRepository.save(mascota);

        // Toda mascota nueva recibe su historia clínica de forma automática
        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(guardada);
        historiaClinicaRepository.save(historia);

        log.info("Mascota registrada: id={}, nombre='{}', propietario_id={}",
                guardada.getIdMascota(), guardada.getNombre(), idPropietario);
        return guardada;
    }

    @Override
    @Transactional
    public Mascota actualizarMascota(Integer idMascota, MascotaDTO dto) {
        Mascota mascota = buscarPorId(idMascota);

        mascota.setNombre(dto.nombre());
        mascota.setEspecie(dto.especie());
        mascota.setRaza(dto.raza());
        mascota.setColor(dto.color());
        mascota.setFechaNacimiento(dto.fechaNacimiento());
        mascota.setSexo(dto.sexo());
        mascota.setPeso(dto.peso());

        Mascota actualizada = mascotaRepository.save(mascota);
        log.info("Mascota actualizada: id={}, nombre='{}'", idMascota, actualizada.getNombre());
        return actualizada;
    }

    @Override
    @Transactional(readOnly = true)
    public Mascota buscarPorId(Integer id) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new MascotaNoEncontradaException(id));
        securityUtils.verificarPropiedadMascota(mascota);
        return mascota;
    }

    @Override
    public List<Mascota> listarPorPropietario(Integer idPropietario) {
        return mascotaRepository.findByPropietario_IdUsuarioAndActivoTrue(idPropietario);
    }

    @Override
    public Page<Mascota> listarPorPropietarioPaginado(Integer idPropietario, Pageable pageable) {
        return mascotaRepository.findByPropietario_IdUsuarioAndActivoTrue(idPropietario, pageable);
    }

    @Override
    public HistoriaClinica verHistorialClinico(Integer idMascota) {
        buscarPorId(idMascota);
        return historiaClinicaRepository.findByMascota_IdMascota(idMascota)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Historia clínica no encontrada para mascota id=" + idMascota));
    }

    @Override
    @Transactional
    public void darDeBaja(Integer idMascota) {
        Mascota mascota = buscarPorId(idMascota);
        mascota.setActivo(false);
        mascotaRepository.save(mascota);
        log.info("Mascota dada de baja: id={}, nombre='{}'", idMascota, mascota.getNombre());
    }

    // ── helpers de mapeo ─────────────────────────────────────────────────────

    private Mascota construirMascota(MascotaDTO dto, Usuario propietario) {
        Mascota mascota = new Mascota();
        mascota.setPropietario(propietario);
        mascota.setNombre(dto.nombre());
        mascota.setEspecie(dto.especie());
        mascota.setRaza(dto.raza());
        mascota.setColor(dto.color());
        mascota.setFechaNacimiento(dto.fechaNacimiento());
        mascota.setSexo(dto.sexo());
        mascota.setPeso(dto.peso());
        mascota.setActivo(true);
        return mascota;
    }
}
