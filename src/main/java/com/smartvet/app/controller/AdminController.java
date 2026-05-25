package com.smartvet.app.controller;

import com.smartvet.app.dto.PagoRegistroDTO;
import com.smartvet.app.dto.ProductoRegistroDTO;
import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.exception.EmailDuplicadoException;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.MetodoPago;
import com.smartvet.app.model.PagoCita;
import com.smartvet.app.model.Producto;
import com.smartvet.app.model.TipoProducto;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.PagoService;
import com.smartvet.app.service.ProductoService;
import com.smartvet.app.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CitaService    citaService;
    private final PagoService    pagoService;
    private final ProductoService productoService;
    private final UsuarioService  usuarioService;

    public AdminController(CitaService citaService,
                           PagoService pagoService,
                           ProductoService productoService,
                           UsuarioService usuarioService) {
        this.citaService     = citaService;
        this.pagoService     = pagoService;
        this.productoService = productoService;
        this.usuarioService  = usuarioService;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Cita>        citasHoy        = citaService.listarCitasDeHoy();
        List<Producto>    stockBajo       = productoService.listarConStockBajo();
        List<Veterinario> veterinarios    = citaService.listarVeterinariosParaCita();
        int               pagosPendientes = pagoService.listarCitasCompletadasSinPago().size();

        model.addAttribute("citasHoy",         citasHoy);
        model.addAttribute("totalCitasHoy",    citasHoy.size());
        model.addAttribute("totalProgramadas", citaService.contarPorEstado(citasHoy, EstadoCita.PROGRAMADA));
        model.addAttribute("totalEnCurso",     citaService.contarPorEstado(citasHoy, EstadoCita.EN_CURSO));
        model.addAttribute("productosStockBajo",  stockBajo);
        model.addAttribute("totalStockBajo",      stockBajo.size());
        model.addAttribute("veterinarios",        veterinarios);
        model.addAttribute("totalVeterinarios",   veterinarios.size());
        model.addAttribute("totalPagosPendientes", pagosPendientes);

        return "admin/dashboard";
    }

    // ── Módulo de pagos ───────────────────────────────────────────────────────

    @GetMapping("/pagos/pendientes")
    public String listarPagosPendientes(Model model) {
        List<Cita> pendientes = pagoService.listarCitasCompletadasSinPago();
        model.addAttribute("citasPendientes", pendientes);
        model.addAttribute("totalPendientes", pendientes.size());
        return "admin/pagos-pendientes";
    }

    @GetMapping("/pagos/registrar/{idCita}")
    public String mostrarFormRegistrarPago(@PathVariable Integer idCita,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        try {
            Cita cita = citaService.buscarPorId(idCita);
            model.addAttribute("cita",    cita);
            model.addAttribute("metodos", MetodoPago.values());
            return "admin/registrar-pago";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/pagos/pendientes";
        }
    }

    @PostMapping("/pagos/registrar/{idCita}")
    public String registrarPago(@PathVariable Integer idCita,
                                 @RequestParam BigDecimal monto,
                                 @RequestParam MetodoPago metodoPago,
                                 @RequestParam(required = false) String referencia,
                                 @RequestParam(required = false) String observaciones,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            PagoRegistroDTO dto = new PagoRegistroDTO(monto, metodoPago, referencia, observaciones);
            PagoCita pago = pagoService.registrarPago(idCita, dto);
            log.info("Pago registrado: id={}, cita_id={}, monto={}, metodo={}",
                    pago.getIdPago(), idCita, monto, metodoPago);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Pago registrado. Cita #" + idCita + " · S/ "
                    + String.format("%.2f", monto) + " · " + metodoPago.name());
            return "redirect:/admin/pagos/pendientes";
        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error al registrar pago para cita_id={}: {}", idCita, ex.getMessage());
            model.addAttribute("errorForm",        ex.getMessage());
            model.addAttribute("metodos",          MetodoPago.values());
            model.addAttribute("montoPrev",        monto);
            model.addAttribute("metodoPrev",       metodoPago);
            model.addAttribute("referenciaPrev",   referencia);
            model.addAttribute("observacionesPrev", observaciones);
            try {
                model.addAttribute("cita", citaService.buscarPorId(idCita));
            } catch (Exception ignored) { /* cita no encontrada, el error ya está en el modelo */ }
            return "admin/registrar-pago";
        }
    }

    // ── Inventario de productos ───────────────────────────────────────────────

    @GetMapping("/productos")
    public String listarInventario(@RequestParam(required = false) String busqueda,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("nombre").ascending());
        Page<Producto> paginaProductos = productoService.listarActivosPaginado(busqueda, pageable);
        long countStockBajo = productoService.listarConStockBajo().size();

        model.addAttribute("productos",       paginaProductos.getContent());
        model.addAttribute("pagina",          paginaProductos);
        model.addAttribute("busqueda",        busqueda != null ? busqueda : "");
        model.addAttribute("countStockBajo",  countStockBajo);
        return "admin/productos";
    }

    @GetMapping("/productos/nuevo")
    public String mostrarFormNuevoProducto(Model model) {
        model.addAttribute("tipos", TipoProducto.values());
        return "admin/nuevo-producto";
    }

    @PostMapping("/productos/nuevo")
    public String registrarProducto(@RequestParam String nombre,
                                     @RequestParam(required = false) String descripcion,
                                     @RequestParam TipoProducto tipoProducto,
                                     @RequestParam(required = false) BigDecimal precioCompra,
                                     @RequestParam BigDecimal precioVenta,
                                     @RequestParam(required = false) Integer stockActual,
                                     @RequestParam(required = false) Integer stockMinimo,
                                     @RequestParam(required = false) String unidadMedida,
                                     @RequestParam(required = false) Boolean requiereReceta,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        try {
            ProductoRegistroDTO dto = new ProductoRegistroDTO(
                    nombre, descripcion, tipoProducto, precioCompra, precioVenta,
                    stockActual, stockMinimo, unidadMedida, requiereReceta);
            Producto guardado = productoService.registrarProducto(dto);
            log.info("Admin registró producto: id={}, nombre='{}'", guardado.getIdProducto(), nombre);
            int stockInicial = stockActual != null ? stockActual : 0;
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Producto \"" + nombre + "\" registrado con " + stockInicial + " unidades en stock.");
            return "redirect:/admin/productos";
        } catch (Exception ex) {
            log.error("Error al registrar producto '{}': {}", nombre, ex.getMessage());
            model.addAttribute("errorForm",          ex.getMessage());
            model.addAttribute("tipos",              TipoProducto.values());
            model.addAttribute("nombrePrev",         nombre);
            model.addAttribute("descripcionPrev",    descripcion);
            model.addAttribute("tipoPrev",           tipoProducto);
            model.addAttribute("precioCompraPrev",   precioCompra);
            model.addAttribute("precioVentaPrev",    precioVenta);
            model.addAttribute("stockActualPrev",    stockActual);
            model.addAttribute("stockMinimoPrev",    stockMinimo);
            model.addAttribute("unidadPrev",         unidadMedida);
            model.addAttribute("requiereRecetaPrev", requiereReceta);
            return "admin/nuevo-producto";
        }
    }

    @PostMapping("/productos/{id}/reponer")
    public String reponerStock(@PathVariable Integer id,
                                @RequestParam int cantidad,
                                RedirectAttributes redirectAttributes) {
        Producto actualizado = productoService.actualizarStock(id, cantidad);
        log.info("Admin repuso stock: producto_id={}, nombre='{}', +{} unidades, nuevo_stock={}",
                id, actualizado.getNombre(), cantidad, actualizado.getStockActual());
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Stock de \"" + actualizado.getNombre() + "\" actualizado. " +
                "Nuevo total: " + actualizado.getStockActual() + " unidades.");
        return "redirect:/admin/productos";
    }

    // ── Reasignación y cancelación de citas ──────────────────────────────────

    @GetMapping("/citas/editar/{id}")
    public String mostrarFormEditarCita(@PathVariable Integer id,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        try {
            Cita cita = citaService.buscarPorId(id);
            List<Veterinario> veterinarios = citaService.listarVeterinariosParaCita();
            model.addAttribute("cita", cita);
            model.addAttribute("veterinarios", veterinarios);
            return "admin/editar-cita";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/citas/editar/{id}")
    public String reasignarVeterinario(@PathVariable Integer id,
                                        @RequestParam Integer idVeterinario,
                                        RedirectAttributes redirectAttributes) {
        try {
            citaService.reasignarVeterinario(id, idVeterinario);
            log.info("Admin reasignó cita_id={} a veterinario_id={}", id, idVeterinario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Cita #" + id + " reasignada correctamente.");
        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error al reasignar cita_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/citas/cancelar/{id}")
    public String cancelarCitaAdmin(@PathVariable Integer id,
                                     RedirectAttributes redirectAttributes) {
        try {
            citaService.cambiarEstado(id, EstadoCita.CANCELADA);
            log.info("Admin canceló cita_id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Cita #" + id + " cancelada.");
        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error al cancelar cita_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // ── Gestión de roles de usuarios ──────────────────────────────────────────

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRolUsuario(@PathVariable Integer id,
                                     @RequestParam String nuevoRol,
                                     RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarRol(id, nuevoRol);
            log.info("Admin cambió rol de usuario_id={} a '{}'", id, nuevoRol);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Rol del usuario actualizado a '" + nuevoRol + "'.");
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al cambiar rol de usuario_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ── Gestión de veterinarios ───────────────────────────────────────────────

    @GetMapping("/veterinarios/nuevo")
    public String mostrarFormNuevoVet(Model model) {
        return "admin/nuevo-veterinario";
    }

    @PostMapping("/veterinarios/nuevo")
    public String crearVeterinario(@RequestParam String email,
                                    @RequestParam String password,
                                    @RequestParam String nombres,
                                    @RequestParam String apellidos,
                                    @RequestParam(required = false) String dni,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam(required = false) String horarioAtencion,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            UsuarioRegistroDTO dto = new UsuarioRegistroDTO(email, password, nombres, apellidos, dni, telefono);
            Usuario creado = usuarioService.registrarVeterinario(dto, horarioAtencion);
            log.info("Admin creó veterinario: usuario_id={}, email={}", creado.getIdUsuario(), email);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Veterinario " + nombres + " " + apellidos + " registrado exitosamente.");
            return "redirect:/admin/dashboard";
        } catch (EmailDuplicadoException ex) {
            log.warn("Admin: email duplicado al crear veterinario: {}", email);
            model.addAttribute("errorForm", ex.getMessage());
            model.addAttribute("emailPrev",    email);
            model.addAttribute("nombresPrev",  nombres);
            model.addAttribute("apellidosPrev", apellidos);
            model.addAttribute("dniPrev",      dni);
            model.addAttribute("telefonoPrev", telefono);
            model.addAttribute("horarioPrev",  horarioAtencion);
            return "admin/nuevo-veterinario";
        }
    }
}
