package com.smartvet.app.service.impl;

import com.smartvet.app.dto.ProductoRegistroDTO;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.exception.StockInsuficienteException;
import com.smartvet.app.model.Producto;
import com.smartvet.app.model.TipoProducto;
import com.smartvet.app.repository.ProductoRepository;
import com.smartvet.app.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public Producto registrarProducto(ProductoRegistroDTO dto) {
        Producto producto = construirProducto(dto);
        Producto guardado = productoRepository.save(producto);

        log.info("Producto registrado: id={}, nombre='{}', tipo={}, stock={}",
                guardado.getIdProducto(), guardado.getNombre(),
                guardado.getTipoProducto(), guardado.getStockActual());

        alertarStockBajoSiCorresponde(guardado);
        return guardado;
    }

    @Override
    public Producto buscarPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto con id=" + id + " no encontrado"));
    }

    @Override
    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrue();
    }

    @Override
    public Page<Producto> listarActivosPaginado(String busqueda, Pageable pageable) {
        if (busqueda != null && !busqueda.isBlank()) {
            return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(busqueda.trim(), pageable);
        }
        return productoRepository.findByActivoTrue(pageable);
    }

    @Override
    public List<Producto> listarPorTipo(TipoProducto tipo) {
        return productoRepository.findByTipoProductoAndActivoTrue(tipo);
    }

    @Override
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre);
    }

    @Override
    public List<Producto> listarConStockBajo() {
        List<Producto> productos = productoRepository.findProductosConStockBajo();
        if (!productos.isEmpty()) {
            log.warn("Alerta de inventario: {} producto(s) con stock bajo o agotado", productos.size());
        }
        return productos;
    }

    @Override
    @Transactional
    public Producto actualizarStock(Integer idProducto, int cantidad) {
        Producto producto = buscarPorId(idProducto);

        int nuevoStock = producto.getStockActual() + cantidad;
        if (nuevoStock < 0) {
            throw new StockInsuficienteException(producto.getNombre(), producto.getStockActual(), Math.abs(cantidad));
        }

        producto.setStockActual(nuevoStock);
        Producto actualizado = productoRepository.save(producto);

        log.info("Stock actualizado: producto='{}', ajuste={}, nuevo_stock={}",
                producto.getNombre(), cantidad, nuevoStock);

        alertarStockBajoSiCorresponde(actualizado);
        return actualizado;
    }

    @Override
    @Transactional
    public void desactivarProducto(Integer id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Producto desactivado: id={}, nombre='{}'", id, producto.getNombre());
    }

    // ── helpers de mapeo y lógica interna ────────────────────────────────────

    private Producto construirProducto(ProductoRegistroDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setTipoProducto(dto.tipoProducto());
        producto.setPrecioCompra(dto.precioCompra());
        producto.setPrecioVenta(dto.precioVenta());
        producto.setStockActual(dto.stockActual() != null ? dto.stockActual() : 0);
        producto.setStockMinimo(dto.stockMinimo() != null ? dto.stockMinimo() : 0);
        producto.setUnidadMedida(dto.unidadMedida());
        producto.setRequiereReceta(dto.requiereReceta() != null ? dto.requiereReceta() : false);
        producto.setActivo(true);
        return producto;
    }

    private void alertarStockBajoSiCorresponde(Producto producto) {
        if (producto.getStockActual() <= producto.getStockMinimo()) {
            log.warn("ALERTA STOCK BAJO — producto='{}' (id={}): actual={}, mínimo={}",
                    producto.getNombre(), producto.getIdProducto(),
                    producto.getStockActual(), producto.getStockMinimo());
        }
    }
}
