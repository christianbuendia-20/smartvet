package com.smartvet.app.service;

import com.smartvet.app.dto.ProductoRegistroDTO;
import com.smartvet.app.model.Producto;
import com.smartvet.app.model.TipoProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {

    Producto registrarProducto(ProductoRegistroDTO dto);

    Producto buscarPorId(Integer id);

    List<Producto> listarActivos();

    Page<Producto> listarActivosPaginado(String busqueda, Pageable pageable);

    List<Producto> listarPorTipo(TipoProducto tipo);

    List<Producto> buscarPorNombre(String nombre);

    List<Producto> listarConStockBajo();

    Producto actualizarStock(Integer idProducto, int cantidad);

    void desactivarProducto(Integer id);
}
