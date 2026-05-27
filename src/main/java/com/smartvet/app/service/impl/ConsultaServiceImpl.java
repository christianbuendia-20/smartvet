package com.smartvet.app.service.impl;

import com.smartvet.app.dto.ConsultaMedicaDTO;
import com.smartvet.app.dto.DetalleRecetaDTO;
import com.smartvet.app.dto.RecetaDTO;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Consulta;
import com.smartvet.app.model.DetalleReceta;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.Producto;
import com.smartvet.app.model.Receta;
import com.smartvet.app.repository.CitaRepository;
import com.smartvet.app.repository.ConsultaRepository;
import com.smartvet.app.repository.DetalleRecetaRepository;
import com.smartvet.app.repository.RecetaRepository;
import com.smartvet.app.service.ConsultaService;
import com.smartvet.app.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ConsultaServiceImpl implements ConsultaService {

    private static final Set<EstadoCita> ESTADOS_VALIDOS_PARA_CONSULTA = Set.of(
            EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA, EstadoCita.EN_CURSO);

    private final ConsultaRepository consultaRepository;
    private final CitaRepository citaRepository;
    private final RecetaRepository recetaRepository;
    private final DetalleRecetaRepository detalleRecetaRepository;
    private final ProductoService productoService;

    public ConsultaServiceImpl(ConsultaRepository consultaRepository,
                                CitaRepository citaRepository,
                                RecetaRepository recetaRepository,
                                DetalleRecetaRepository detalleRecetaRepository,
                                ProductoService productoService) {
        this.consultaRepository = consultaRepository;
        this.citaRepository = citaRepository;
        this.recetaRepository = recetaRepository;
        this.detalleRecetaRepository = detalleRecetaRepository;
        this.productoService = productoService;
    }

    @Override
    @Transactional
    public Consulta registrarConsulta(ConsultaMedicaDTO dto) {
        Cita cita = citaRepository.findById(dto.idCita())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cita con id=" + dto.idCita() + " no encontrada"));

        validarCitaParaConsulta(cita);

        Consulta consulta = construirConsulta(dto, cita);
        Consulta guardada = consultaRepository.save(consulta);

        if (dto.receta() != null) {
            generarReceta(dto.receta(), guardada);
        }

        // Cierra la cita al registrar la consulta
        cita.setEstado(EstadoCita.COMPLETADA);
        citaRepository.save(cita);

        log.info("Consulta registrada: id={}, cita_id={}, mascota='{}', con_receta={}",
                guardada.getIdConsulta(), dto.idCita(),
                cita.getMascota().getNombre(), dto.receta() != null);
        return guardada;
    }

    @Override
    public Consulta buscarPorId(Integer id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Consulta con id=" + id + " no encontrada"));
    }

    @Override
    public Consulta buscarPorCita(Integer idCita) {
        return consultaRepository.findByCita_IdCita(idCita)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe consulta registrada para la cita id=" + idCita));
    }

    @Override
    public List<Consulta> listarPorMascota(Integer idMascota) {
        return consultaRepository.findByMascotaConDetalle(idMascota);
    }

    @Override
    @Transactional
    public Consulta actualizarConsulta(Integer idConsulta,
                                       String diagnostico,
                                       String tratamiento,
                                       String observaciones,
                                       BigDecimal temperatura,
                                       BigDecimal peso,
                                       Integer frecuenciaCardiaca) {
        Consulta consulta = buscarPorId(idConsulta);
        if (!estaEnVentanaEdicion(consulta)) {
            throw new EstadoInvalidoException(
                    "La consulta ya no puede editarse: han transcurrido más de 60 minutos desde su registro.");
        }
        consulta.setDiagnostico(diagnostico);
        consulta.setTratamiento(tratamiento);
        consulta.setObservaciones(observaciones);
        consulta.setTemperatura(temperatura);
        consulta.setPeso(peso);
        consulta.setFrecuenciaCardiaca(frecuenciaCardiaca);
        Consulta guardada = consultaRepository.save(consulta);
        log.info("Consulta actualizada: id={}", guardada.getIdConsulta());
        return guardada;
    }

    @Override
    public boolean estaEnVentanaEdicion(Consulta consulta) {
        return Duration.between(consulta.getFechaHora(), LocalDateTime.now()).toMinutes() <= 60L;
    }

    // ── lógica de negocio interna ─────────────────────────────────────────────

    private void validarCitaParaConsulta(Cita cita) {
        if (!ESTADOS_VALIDOS_PARA_CONSULTA.contains(cita.getEstado())) {
            throw new EstadoInvalidoException(String.format(
                    "No se puede registrar una consulta para una cita en estado %s. " +
                    "Estados permitidos: %s", cita.getEstado(), ESTADOS_VALIDOS_PARA_CONSULTA));
        }
    }

    private Consulta construirConsulta(ConsultaMedicaDTO dto, Cita cita) {
        Consulta consulta = new Consulta();
        consulta.setCita(cita);
        consulta.setVeterinario(cita.getVeterinario());
        consulta.setMascota(cita.getMascota());
        consulta.setFechaHora(dto.fechaHora());
        consulta.setTemperatura(dto.temperatura());
        consulta.setPeso(dto.peso());
        consulta.setFrecuenciaCardiaca(dto.frecuenciaCardiaca());
        consulta.setDiagnostico(dto.diagnostico());
        consulta.setTratamiento(dto.tratamiento());
        consulta.setObservaciones(dto.observaciones());
        return consulta;
    }

    private void generarReceta(RecetaDTO recetaDTO, Consulta consulta) {
        Receta receta = new Receta();
        receta.setConsulta(consulta);
        receta.setInstrucciones(recetaDTO.instrucciones());
        receta.setVigenciaDias(recetaDTO.vigenciaDias() != null ? recetaDTO.vigenciaDias() : 30);
        Receta recetaGuardada = recetaRepository.save(receta);

        for (DetalleRecetaDTO detalleDTO : recetaDTO.detalles()) {
            registrarDetalleYDescontarStock(detalleDTO, recetaGuardada, consulta.getIdConsulta());
        }

        log.info("Receta generada: id={}, consulta_id={}, medicamentos={}",
                recetaGuardada.getIdReceta(), consulta.getIdConsulta(), recetaDTO.detalles().size());
    }

    private void registrarDetalleYDescontarStock(DetalleRecetaDTO detalleDTO,
                                                  Receta receta,
                                                  Integer idConsulta) {
        // actualizarStock lanza StockInsuficienteException si no hay stock —
        // al estar en la misma transacción, revertirá toda la consulta
        Producto producto = productoService.actualizarStock(detalleDTO.idProducto(), -detalleDTO.cantidad());

        DetalleReceta detalle = new DetalleReceta();
        detalle.setReceta(receta);
        detalle.setProducto(producto);
        detalle.setCantidad(detalleDTO.cantidad());
        detalle.setDosis(detalleDTO.dosis());
        detalle.setFrecuencia(detalleDTO.frecuencia());
        detalle.setDuracionDias(detalleDTO.duracionDias());
        detalle.setInstruccionesAdicionales(detalleDTO.instruccionesAdicionales());
        detalleRecetaRepository.save(detalle);

        log.info("Medicamento recetado: producto='{}', cantidad={}, consulta_id={}",
                producto.getNombre(), detalleDTO.cantidad(), idConsulta);
    }
}
