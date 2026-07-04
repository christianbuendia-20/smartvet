package com.smartvet.app.controller;

import com.smartvet.app.dto.CitaAgendamientoDTO;
import com.smartvet.app.dto.PagoFilaDTO;
import com.smartvet.app.dto.PagoRegistroDTO;
import com.smartvet.app.dto.PerfilUpdateDTO;
import com.smartvet.app.dto.ProductoRegistroDTO;
import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.dto.VeterinarioFormDTO;
import com.smartvet.app.exception.EmailDuplicadoException;
import com.smartvet.app.exception.EstadoInvalidoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Consulta;
import com.smartvet.app.model.EstadoCita;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.MetodoPago;
import com.smartvet.app.model.PagoCita;
import com.smartvet.app.model.Producto;
import com.smartvet.app.model.TipoCita;
import com.smartvet.app.model.TipoProducto;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.service.CitaService;
import com.smartvet.app.service.ConsultaPdfService;
import com.smartvet.app.service.ConsultaService;
import com.smartvet.app.service.EmailService;
import com.smartvet.app.service.MascotaService;
import com.smartvet.app.service.PagoService;
import com.smartvet.app.service.ProductoService;
import com.smartvet.app.service.UsuarioService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final CitaService        citaService;
    private final MascotaService     mascotaService;
    private final PagoService        pagoService;
    private final ProductoService    productoService;
    private final UsuarioService     usuarioService;
    private final ConsultaService    consultaService;
    private final ConsultaPdfService consultaPdfService;
    private final EmailService       emailService;
    private final String             mpPublicKey;

    public AdminController(CitaService citaService,
                           MascotaService mascotaService,
                           PagoService pagoService,
                           ProductoService productoService,
                           UsuarioService usuarioService,
                           ConsultaService consultaService,
                           ConsultaPdfService consultaPdfService,
                           EmailService emailService,
                           @Value("${mercadopago.public-key}") String mpPublicKey) {
        this.citaService        = citaService;
        this.mascotaService     = mascotaService;
        this.pagoService        = pagoService;
        this.productoService    = productoService;
        this.usuarioService     = usuarioService;
        this.consultaService    = consultaService;
        this.consultaPdfService = consultaPdfService;
        this.emailService       = emailService;
        this.mpPublicKey        = mpPublicKey;
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Cita>        citasHoy        = citaService.listarCitasDeHoy();
        List<Cita>        citasManana     = citaService.listarCitasDeManana();
        List<Producto>    stockBajo       = productoService.listarConStockBajo();
        List<Veterinario> veterinarios    = citaService.listarVeterinariosParaCita();
        int               pagosPendientes = pagoService.listarCitasCompletadasSinPago().size();

        model.addAttribute("citasHoy",             citasHoy);
        model.addAttribute("citasManana",          citasManana);
        model.addAttribute("totalCitasHoy",        citasHoy.size());
        model.addAttribute("totalProgramadas",     citaService.contarPorEstado(citasHoy, EstadoCita.PROGRAMADA));
        model.addAttribute("totalEnCurso",         citaService.contarPorEstado(citasHoy, EstadoCita.EN_CURSO));
        model.addAttribute("productosStockBajo",   stockBajo);
        model.addAttribute("totalStockBajo",       stockBajo.size());
        model.addAttribute("veterinarios",         veterinarios);
        model.addAttribute("totalVeterinarios",    veterinarios.size());
        model.addAttribute("totalPagosPendientes", pagosPendientes);

        return "admin/dashboard";
    }

    // ── Módulo de pagos ───────────────────────────────────────────────────────

    @GetMapping("/pagos")
    public String listarPagos(Model model) {
        List<PagoFilaDTO> filas      = pagoService.listarParaVista();
        long              pendientes = filas.stream()
                .filter(PagoFilaDTO::isPendiente).count();
        LocalDateTime inicioMes   = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal    recaudacion = pagoService.calcularRecaudacion(inicioMes, LocalDateTime.now());

        model.addAttribute("pagos",           filas);
        model.addAttribute("totalPagos",      filas.size());
        model.addAttribute("totalPendientes", pendientes);
        model.addAttribute("recaudacionMes",  recaudacion);
        return "admin/pagos";
    }

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
            model.addAttribute("cita",       cita);
            model.addAttribute("metodos",    MetodoPago.values());
            model.addAttribute("mpPublicKey", mpPublicKey);
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
                    "Pago registrado correctamente. Cita #" + idCita + " · S/ "
                    + String.format("%.2f", monto) + " · " + metodoPago.name());
            return "redirect:/admin/pagos";
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
        Page<Producto> paginaProductos = productoService.listarTodosPaginado(busqueda, pageable);
        long countStockBajo = productoService.listarConStockBajo().size();

        model.addAttribute("productos",       paginaProductos.getContent());
        model.addAttribute("pagina",          paginaProductos);
        model.addAttribute("busqueda",        busqueda != null ? busqueda : "");
        model.addAttribute("countStockBajo",  countStockBajo);
        return "admin/productos";
    }

    @GetMapping("/productos/{id}")
    public String verProducto(@PathVariable Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("producto", productoService.buscarPorId(id));
            return "admin/ver-producto";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/productos";
        }
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

    @GetMapping("/productos/{id}/editar")
    public String mostrarFormEditarProducto(@PathVariable Integer id,
                                             Model model,
                                             RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("producto", productoService.buscarPorId(id));
            model.addAttribute("tipos",    TipoProducto.values());
            return "admin/editar-producto";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/productos";
        }
    }

    @PostMapping("/productos/{id}/editar")
    public String editarProducto(@PathVariable Integer id,
                                  @RequestParam String nombre,
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
            productoService.actualizarProducto(id, dto);
            log.info("Admin editó producto: id={}, nombre='{}'", id, nombre);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Producto \"" + nombre + "\" actualizado correctamente.");
            return "redirect:/admin/productos";
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al editar producto id={}: {}", id, ex.getMessage());
            model.addAttribute("errorForm", ex.getMessage());
            try { model.addAttribute("producto", productoService.buscarPorId(id)); }
            catch (Exception ignored) {}
            model.addAttribute("tipos", TipoProducto.values());
            return "admin/editar-producto";
        }
    }

    @PostMapping("/productos/{id}/desactivar")
    public String desactivarProducto(@PathVariable Integer id,
                                      RedirectAttributes redirectAttributes) {
        try {
            productoService.desactivarProducto(id);
            log.info("Admin desactivó producto: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Producto #" + id + " desactivado. Ya no aparece en búsquedas activas.");
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al desactivar producto id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/activar")
    public String activarProducto(@PathVariable Integer id,
                                   RedirectAttributes redirectAttributes) {
        try {
            productoService.activarProducto(id);
            log.info("Admin activó producto: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Producto #" + id + " reactivado.");
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al activar producto id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/productos";
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
            return "redirect:/admin/citas";
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
        return "redirect:/admin/citas";
    }

    @PostMapping("/citas/cancelar/{id}")
    public String cancelarCitaAdmin(@PathVariable Integer id,
                                     RedirectAttributes redirectAttributes) {
        try {
            citaService.cambiarEstado(id, EstadoCita.CANCELADA);
            log.info("Admin canceló cita_id={}", id);
            emailService.enviarCancelacionCita(id);
            redirectAttributes.addFlashAttribute("mensajeExito", "Cita #" + id + " cancelada.");
        } catch (EstadoInvalidoException | RecursoNoEncontradoException ex) {
            log.warn("Error al cancelar cita_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/citas";
    }

    // ── Gestión de roles de usuarios ──────────────────────────────────────────

    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(value = "keyword", required = false) String keyword,
                                  Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos(keyword));
        model.addAttribute("keyword",  keyword != null ? keyword : "");
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/{id}")
    public String verUsuario(@PathVariable Integer id,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("usuario",   usuarioService.buscarPorId(id));
            model.addAttribute("direccion", usuarioService.buscarDireccion(id).orElse(null));
            return "admin/ver-usuario";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/usuarios";
        }
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

    // ── Creación general de usuarios ──────────────────────────────────────────

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormNuevoUsuario(Model model) {
        model.addAttribute("roles", List.of("cliente", "veterinario", "admin"));
        return "admin/nuevo-usuario";
    }

    @PostMapping("/usuarios/nuevo")
    public String crearUsuario(@RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String nombres,
                                @RequestParam String apellidos,
                                @RequestParam(required = false) String dni,
                                @RequestParam(required = false) String telefono,
                                @RequestParam String rol,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            UsuarioRegistroDTO dto = new UsuarioRegistroDTO(email, password, nombres, apellidos, dni, telefono);
            Usuario creado = usuarioService.registrarConRol(dto, rol);
            log.info("Admin creó usuario con rol '{}': usuario_id={}, email={}", rol, creado.getIdUsuario(), email);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Usuario " + nombres + " " + apellidos + " registrado con rol '" + rol + "'.");
            return "redirect:/admin/usuarios";
        } catch (EmailDuplicadoException ex) {
            log.warn("Admin: email duplicado al crear usuario: {}", email);
            return volverAFormularioNuevoUsuario(model, ex.getMessage(), email, nombres, apellidos, dni, telefono, rol);
        } catch (Exception ex) {
            log.error("Error inesperado al crear usuario '{}': {}", email, ex.getMessage());
            String msg = "No se pudo crear el usuario. Es posible que el correo o el DNI ya estén registrados.";
            return volverAFormularioNuevoUsuario(model, msg, email, nombres, apellidos, dni, telefono, rol);
        }
    }

    private String volverAFormularioNuevoUsuario(Model model, String error,
                                                  String email, String nombres, String apellidos,
                                                  String dni, String telefono, String rol) {
        model.addAttribute("errorForm",     error);
        model.addAttribute("roles",         List.of("cliente", "veterinario", "admin"));
        model.addAttribute("emailPrev",     email);
        model.addAttribute("nombresPrev",   nombres);
        model.addAttribute("apellidosPrev", apellidos);
        model.addAttribute("dniPrev",       dni);
        model.addAttribute("telefonoPrev",  telefono);
        model.addAttribute("rolPrev",       rol);
        return "admin/nuevo-usuario";
    }

    // ── Gestión de veterinarios ───────────────────────────────────────────────

    // ── Listado completo de citas ─────────────────────────────────────────────

    @GetMapping("/citas")
    public String listarTodasCitas(@RequestParam(value = "keyword", required = false) String keyword,
                                    @RequestParam(value = "fecha", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                    Model model) {
        model.addAttribute("citas",   citaService.listarTodas(keyword, fecha));
        model.addAttribute("estados", EstadoCita.values());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("fecha",   fecha);
        return "admin/citas";
    }

    @GetMapping("/citas/{id}")
    public String verCita(@PathVariable Integer id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Cita cita = citaService.buscarPorId(id);
            model.addAttribute("cita", cita);
            try {
                model.addAttribute("consulta", consultaService.buscarPorCita(id));
            } catch (RecursoNoEncontradoException ignored) {
                // cita sin consulta asociada aún
            }
            return "admin/ver-cita";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/citas";
        }
    }

    @GetMapping("/consultas/{id}/pdf/descargar")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        try {
            byte[] pdf = consultaPdfService.generarPdf(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"Consulta_" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RecursoNoEncontradoException ex) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException ex) {
            log.error("Error al generar PDF consulta_id={}", id, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/consultas/{id}/pdf/enviar")
    public String enviarPdfPorCorreo(@PathVariable Integer id,
                                      RedirectAttributes redirectAttributes) {
        Integer citaId = null;
        try {
            Consulta consulta = consultaService.buscarPorId(id);
            citaId = consulta.getCita().getIdCita();
            byte[] pdf = consultaPdfService.generarPdf(id);
            Usuario propietario = consulta.getMascota().getPropietario();
            emailService.enviarConsultaPdf(
                    propietario.getEmail(),
                    propietario.getNombres() + " " + propietario.getApellidos(),
                    pdf, id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "PDF enviado exitosamente a " + propietario.getEmail()
                    + ". Indíquele al cliente que revise su carpeta de Spam si no lo visualiza en unos minutos.");
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", "Consulta no encontrada.");
        } catch (RuntimeException ex) {
            log.error("Error al enviar PDF consulta_id={}", id, ex);
            redirectAttributes.addFlashAttribute("errorForm",
                    "No se pudo enviar el correo. Verifique la configuración SMTP.");
        }
        return citaId != null ? "redirect:/admin/citas/" + citaId : "redirect:/admin/citas";
    }

    // ── Agendar nueva cita (en nombre de un cliente) ──────────────────────────

    @GetMapping("/citas/nueva")
    public String mostrarFormNuevaCita(Model model) {
        List<Usuario>    clientes     = usuarioService.listarClientes();
        List<Mascota>    mascotas     = mascotaService.listarTodas();
        List<Veterinario> veterinarios = citaService.listarVeterinariosParaCita();
        model.addAttribute("clientes",     clientes);
        model.addAttribute("mascotas",     mascotas);
        model.addAttribute("veterinarios", veterinarios);
        model.addAttribute("tipos",        TipoCita.values());
        return "admin/nueva-cita";
    }

    @PostMapping("/citas/nueva")
    public String agendarCitaAdmin(@RequestParam Integer idMascota,
                                    @RequestParam Integer idVeterinario,
                                    @RequestParam TipoCita tipoCita,
                                    @RequestParam String fechaHora,
                                    @RequestParam String motivo,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            if (citaService.contarCitasActivasPorMascota(idMascota) >= 3) {
                redirectAttributes.addFlashAttribute("error",
                        "Límite de citas alcanzado: El cliente ya tiene 3 citas activas. " +
                        "Debe cancelar o finalizar una cita existente antes de registrar una nueva.");
                return "redirect:/admin/citas/nueva";
            }

            LocalDateTime fechaHoraDT = LocalDateTime.parse(
                    fechaHora.length() == 16 ? fechaHora + ":00" : fechaHora);

            LocalDateTime fechaMaxima = LocalDateTime.now().plusDays(15);
            if (fechaHoraDT.isAfter(fechaMaxima)) {
                redirectAttributes.addFlashAttribute("error",
                        "No se pueden programar citas con más de 15 días de anticipación.");
                return "redirect:/admin/citas/nueva";
            }

            CitaAgendamientoDTO dto = new CitaAgendamientoDTO(idMascota, idVeterinario, tipoCita, fechaHoraDT, motivo);
            Cita guardada = citaService.programarCita(dto);
            log.info("Admin agendó cita: id={}, mascota_id={}, veterinario_id={}",
                    guardada.getIdCita(), idMascota, idVeterinario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Cita #" + guardada.getIdCita() + " agendada correctamente.");
            return "redirect:/admin/citas";
        } catch (Exception ex) {
            log.warn("Error al agendar cita: {}", ex.getMessage());
            model.addAttribute("errorForm",    ex.getMessage());
            model.addAttribute("clientes",     usuarioService.listarClientes());
            model.addAttribute("mascotas",     mascotaService.listarTodas());
            model.addAttribute("veterinarios", citaService.listarVeterinariosParaCita());
            model.addAttribute("tipos",        TipoCita.values());
            model.addAttribute("idMascotaPrev",     idMascota);
            model.addAttribute("idVeterinarioPrev", idVeterinario);
            model.addAttribute("tipoPrev",          tipoCita);
            model.addAttribute("fechaHoraPrev",     fechaHora);
            model.addAttribute("motivoPrev",        motivo);
            return "admin/nueva-cita";
        }
    }

    // ── Editar datos personales de un usuario ─────────────────────────────────

    @GetMapping("/usuarios/{id}/editar")
    public String mostrarFormEditarUsuario(@PathVariable Integer id,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            model.addAttribute("usuario",        usuario);
            model.addAttribute("direccionActual", usuarioService.buscarDireccion(id).orElse(null));
            model.addAttribute("roles",           List.of("cliente", "veterinario", "admin"));
            return "admin/editar-usuario";
        } catch (RecursoNoEncontradoException ex) {
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/usuarios/{id}/editar")
    public String editarUsuario(@PathVariable Integer id,
                                 @RequestParam String nombres,
                                 @RequestParam String apellidos,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam(required = false) String dni,
                                 @RequestParam(required = false) String direccion,
                                 @RequestParam(required = false) String referencia,
                                 @RequestParam(required = false) String nuevoRol,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            PerfilUpdateDTO dto = new PerfilUpdateDTO(nombres, apellidos, email, telefono, dni, direccion, referencia);
            usuarioService.actualizarPerfil(id, dto);
            if (nuevoRol != null && !nuevoRol.isBlank()) {
                usuarioService.cambiarRol(id, nuevoRol);
            }
            log.info("Admin editó usuario: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Datos de " + nombres + " " + apellidos + " actualizados.");
            return "redirect:/admin/usuarios";
        } catch (EmailDuplicadoException ex) {
            log.warn("Admin: email duplicado al editar usuario_id={}: {}", id, ex.getMessage());
            model.addAttribute("errorForm",      ex.getMessage());
            model.addAttribute("direccionActual", usuarioService.buscarDireccion(id).orElse(null));
            model.addAttribute("roles",           List.of("cliente", "veterinario", "admin"));
            try {
                model.addAttribute("usuario", usuarioService.buscarPorId(id));
            } catch (Exception ignored) { /* no encontrado */ }
            return "admin/editar-usuario";
        }
    }

    // ── Desactivar / Activar usuario ─────────────────────────────────────────

    @PostMapping("/usuarios/{id}/desactivar")
    public String desactivarUsuario(@PathVariable Integer id,
                                     RedirectAttributes redirectAttributes) {
        try {
            usuarioService.desactivarUsuario(id);
            log.info("Admin desactivó usuario: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Usuario #" + id + " desactivado. Ya no podrá iniciar sesión.");
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al desactivar usuario_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/activar")
    public String activarUsuario(@PathVariable Integer id,
                                  RedirectAttributes redirectAttributes) {
        try {
            usuarioService.activarUsuario(id);
            log.info("Admin activó usuario: id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Usuario #" + id + " reactivado. Ya puede iniciar sesión.");
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al activar usuario_id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ── Gestión de veterinarios ───────────────────────────────────────────────

    @GetMapping("/veterinarios/nuevo")
    public String mostrarFormPerfilVet(Model model) {
        model.addAttribute("veterinario",       new VeterinarioFormDTO());
        model.addAttribute("listaVeterinarios", usuarioService.listarVeterinarios());
        return "admin/nuevo-veterinario";
    }

    @GetMapping("/veterinarios/editar/{id}")
    public String mostrarFormEditarVet(@PathVariable Integer id,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        try {
            Usuario   usuario = usuarioService.buscarPorId(id);
            Veterinario vet   = usuarioService.buscarVeterinarioPorIdUsuario(id);
            VeterinarioFormDTO form = new VeterinarioFormDTO();
            form.setId(usuario.getIdUsuario());
            form.setNombres(usuario.getNombres());
            form.setApellidos(usuario.getApellidos());
            form.setHorarioAtencion(vet.getHorarioAtencion());
            model.addAttribute("veterinario",       form);
            model.addAttribute("listaVeterinarios", usuarioService.listarVeterinarios());
            return "admin/nuevo-veterinario";
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al cargar perfil veterinario id={}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorForm", ex.getMessage());
            return "redirect:/admin/veterinarios/nuevo";
        }
    }

    @PostMapping("/veterinarios/nuevo")
    public String guardarPerfilVeterinario(@RequestParam(required = false) Integer id,
                                            @RequestParam(required = false) String horarioAtencion,
                                            RedirectAttributes redirectAttributes,
                                            Model model) {
        if (id == null) {
            model.addAttribute("errorForm", "Debe seleccionar un veterinario de la lista.");
            model.addAttribute("veterinario",       new VeterinarioFormDTO());
            model.addAttribute("listaVeterinarios", usuarioService.listarVeterinarios());
            return "admin/nuevo-veterinario";
        }
        try {
            usuarioService.actualizarHorarioVeterinario(id, horarioAtencion);
            log.info("Admin actualizó horario de veterinario: usuario_id={}", id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Perfil médico actualizado correctamente.");
            return "redirect:/admin/dashboard";
        } catch (RecursoNoEncontradoException ex) {
            log.warn("Error al guardar perfil veterinario id={}: {}", id, ex.getMessage());
            model.addAttribute("errorForm", ex.getMessage());
            VeterinarioFormDTO form = new VeterinarioFormDTO();
            form.setId(id);
            form.setHorarioAtencion(horarioAtencion);
            model.addAttribute("veterinario",       form);
            model.addAttribute("listaVeterinarios", usuarioService.listarVeterinarios());
            return "admin/nuevo-veterinario";
        }
    }
}
