package com.smartvet.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detalle_receta")
public class DetalleReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_receta")
    private Integer idDetalleReceta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_receta", nullable = false)
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "dosis", length = 100)
    private String dosis;

    @Column(name = "frecuencia", length = 100)
    private String frecuencia;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(name = "instrucciones_adicionales", columnDefinition = "TEXT")
    private String instruccionesAdicionales;
}
