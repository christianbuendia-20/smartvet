package com.smartvet.app.service.impl;

import com.smartvet.app.dto.PagoFilaDTO;
import com.smartvet.app.dto.PagoRegistroDTO;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.EstadoPago;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.PagoCita;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.repository.CitaRepository;
import com.smartvet.app.repository.PagoCitaRepository;
import com.smartvet.app.service.PagoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class PagoServiceImpl implements PagoService {

    private static final Set<EstadoCita> ESTADOS_PAGABLES = Set.of(
            EstadoCita.EN_CURSO, EstadoCita.COMPLETADA);

    private final PagoCitaRepository pagoCitaRepository;
    private final CitaRepository citaRepository;

    public PagoServiceImpl(PagoCitaRepository pagoCitaRepository,
                            CitaRepository citaRepository) {
        this.pagoCitaRepository = pagoCitaRepository;
        this.citaRepository = citaRepository;
    }

    @Override
    @Transactional
    public PagoCita registrarPago(Integer idCita, PagoRegistroDTO dto) {
        Cita cita = citaRepository.findById(idCita)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cita con id=" + idCita + " no encontrada"));

        if (!ESTADOS_PAGABLES.contains(cita.getEstado())) {
            throw new EstadoInvalidoException(String.format(
                    "No se puede registrar un pago para una cita en estado %s. " +
                    "Estados permitidos: %s", cita.getEstado(), ESTADOS_PAGABLES));
        }

        PagoCita pago = new PagoCita();
        pago.setCita(cita);
        pago.setMonto(dto.monto());
        pago.setMetodoPago(dto.metodoPago());
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setReferencia(dto.referencia());
        pago.setObservaciones(dto.observaciones());

        PagoCita guardado = pagoCitaRepository.save(pago);
        log.info("Pago registrado: id={}, cita_id={}, monto={}, metodo={}",
                guardado.getIdPago(), idCita, dto.monto(), dto.metodoPago());
        return guardado;
    }

    @Override
    public PagoCita buscarPorId(Integer id) {
        return pagoCitaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago con id=" + id + " no encontrado"));
    }

    @Override
    public List<PagoCita> listarPorCita(Integer idCita) {
        return pagoCitaRepository.findByCita_IdCita(idCita);
    }

    @Override
    public List<Cita> listarCitasCompletadasSinPago() {
        return citaRepository.findCitasCompletadasSinPago(EstadoCita.COMPLETADA, EstadoPago.COMPLETADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoCita> listarTodos() {
        return pagoCitaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPago"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoFilaDTO> listarParaVista() {
        List<PagoFilaDTO> filas = new ArrayList<>();

        for (PagoCita p : pagoCitaRepository.findAllWithDetalle()) {
            Mascota  m   = p.getCita().getMascota();
            Usuario  pro = m.getPropietario();
            filas.add(new PagoFilaDTO(
                    p.getIdPago(),
                    p.getCita().getIdCita(),
                    p.getCita().getFechaHora(),
                    p.getFechaPago(),
                    m.getNombre(),
                    m.getEspecie() != null ? m.getEspecie().toString() : "—",
                    pro.getNombres() + " " + pro.getApellidos(),
                    p.getMonto(),
                    p.getMetodoPago() != null ? p.getMetodoPago().name() : null,
                    p.getReferencia(),
                    p.getEstado()
            ));
        }

        for (Cita c : citaRepository.findCitasCompletadasSinPago(EstadoCita.COMPLETADA, EstadoPago.COMPLETADO)) {
            Mascota  m   = c.getMascota();
            Usuario  pro = m.getPropietario();
            filas.add(new PagoFilaDTO(
                    null,
                    c.getIdCita(),
                    c.getFechaHora(),
                    null,
                    m.getNombre(),
                    m.getEspecie() != null ? m.getEspecie().toString() : "—",
                    pro.getNombres() + " " + pro.getApellidos(),
                    null,
                    null,
                    null,
                    EstadoPago.PENDIENTE
            ));
        }

        return filas;
    }

    @Override
    public BigDecimal calcularRecaudacion(LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal total = pagoCitaRepository.sumMontoByFechaPagoBetween(inicio, fin);
        BigDecimal resultado = total != null ? total : BigDecimal.ZERO;
        log.info("Recaudación calculada: periodo={} → {}, total={}", inicio, fin, resultado);
        return resultado;
    }
}
