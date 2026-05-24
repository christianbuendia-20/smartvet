package com.smartvet.app.service;

import com.smartvet.app.dto.MascotaDTO;
import com.smartvet.app.model.HistoriaClinica;
import com.smartvet.app.model.Mascota;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MascotaService {

    Mascota registrarMascota(MascotaDTO dto, Integer idPropietario);

    Mascota actualizarMascota(Integer idMascota, MascotaDTO dto);

    Mascota buscarPorId(Integer id);

    List<Mascota> listarPorPropietario(Integer idPropietario);

    Page<Mascota> listarPorPropietarioPaginado(Integer idPropietario, Pageable pageable);

    HistoriaClinica verHistorialClinico(Integer idMascota);

    void darDeBaja(Integer idMascota);
}
