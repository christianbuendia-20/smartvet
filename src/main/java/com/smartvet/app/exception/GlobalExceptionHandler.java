package com.smartvet.app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ── Excepciones de dominio: entidades no encontradas ──────────────────────

    @ExceptionHandler(MascotaNoEncontradaException.class)
    public ModelAndView handleMascotaNoEncontrada(MascotaNoEncontradaException ex) {
        log.warn("Mascota no encontrada: {}", ex.getMessage());
        return buildErrorView(
                "Mascota no encontrada",
                ex.getMessage(),
                "Verifique que el identificador de la mascota sea correcto.",
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildErrorView(
                "Recurso no encontrado",
                ex.getMessage(),
                "El elemento solicitado no existe o fue eliminado.",
                HttpStatus.NOT_FOUND);
    }

    // ── Excepciones de dominio: reglas de negocio ─────────────────────────────

    @ExceptionHandler(EmailDuplicadoException.class)
    public ModelAndView handleEmailDuplicado(EmailDuplicadoException ex) {
        log.warn("Registro con email duplicado: {}", ex.getMessage());
        return buildErrorView(
                "Email ya registrado",
                ex.getMessage(),
                "Utilice un correo electrónico diferente o inicie sesión con su cuenta existente.",
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CitaNoDisponibleException.class)
    public ModelAndView handleCitaNoDisponible(CitaNoDisponibleException ex) {
        log.warn("Conflicto de agenda: {}", ex.getMessage());
        return buildErrorView(
                "Horario no disponible",
                ex.getMessage(),
                "Seleccione otro horario o un veterinario diferente para continuar.",
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LimiteCitasException.class)
    public ModelAndView handleLimiteCitas(LimiteCitasException ex) {
        log.warn("Límite de citas activas excedido: {}", ex.getMessage());
        return buildErrorView(
                "Límite de citas alcanzado",
                ex.getMessage(),
                "Cancela o finaliza una cita existente antes de registrar una nueva.",
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ModelAndView handleEstadoInvalido(EstadoInvalidoException ex) {
        log.warn("Transición de estado inválida: {}", ex.getMessage());
        return buildErrorView(
                "Operación no permitida",
                ex.getMessage(),
                "La acción solicitada no es válida para el estado actual del recurso.",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ModelAndView handleStockInsuficiente(StockInsuficienteException ex) {
        log.warn("Stock insuficiente al generar receta: {}", ex.getMessage());
        return buildErrorView(
                "Stock insuficiente",
                ex.getMessage(),
                "Reponga el inventario del medicamento antes de registrar la receta.",
                HttpStatus.CONFLICT);
    }

    // ── Excepciones de autorización / propiedad ───────────────────────────────

    @ExceptionHandler(AccesoDenegadoException.class)
    public ModelAndView handleAccesoDenegado(AccesoDenegadoException ex) {
        log.warn("Acceso denegado a recurso protegido: {}", ex.getMessage());
        return buildErrorView(
                "Acceso Denegado",
                ex.getMessage(),
                "Solo puedes ver o modificar recursos que te pertenecen. " +
                "Si crees que esto es un error, contacta al administrador.",
                HttpStatus.FORBIDDEN);
    }

    // ── Excepciones de autenticación ──────────────────────────────────────────

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ModelAndView handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Intento de acceso con credenciales inválidas");
        return buildErrorView(
                "Acceso denegado",
                ex.getMessage(),
                "Verifique su email y contraseña, e inténtelo de nuevo.",
                HttpStatus.UNAUTHORIZED);
    }

    // ── Recurso estático no encontrado (404 de Spring MVC) ───────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Ruta no encontrada: {}", ex.getMessage());
        return buildErrorView(
                "Página no encontrada",
                "La dirección que intentó acceder no existe.",
                "Verifique la URL o regrese al inicio.",
                HttpStatus.NOT_FOUND);
    }

    // ── Catch-all: errores inesperados ────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        log.error("Error inesperado del sistema", ex);
        return buildErrorView(
                "Error del sistema",
                "Ha ocurrido un error inesperado.",
                "Por favor, inténtelo de nuevo en unos momentos o contacte al administrador.",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private ModelAndView buildErrorView(String titulo, String mensaje, String sugerencia, HttpStatus status) {
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("titulo", titulo);
        mav.addObject("mensaje", mensaje);
        mav.addObject("sugerencia", sugerencia);
        mav.addObject("codigo", status.value());
        mav.setStatus(status);
        return mav;
    }
}
