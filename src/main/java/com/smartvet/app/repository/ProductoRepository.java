package com.smartvet.app.repository;

import com.smartvet.app.model.Producto;
import com.smartvet.app.model.TipoProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByActivoTrue();

    Page<Producto> findByActivoTrue(Pageable pageable);

    List<Producto> findByTipoProductoAndActivoTrue(TipoProducto tipoProducto);

    List<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    Page<Producto> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre, Pageable pageable);

    Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Producto> findByRequiereRecetaTrue();

    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo AND p.activo = true")
    List<Producto> findProductosConStockBajo();
}
