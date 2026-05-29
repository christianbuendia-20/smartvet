package com.smartvet.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VeterinarioFormDTO {
    private Integer id;
    private String  email;
    private String  nombres;
    private String  apellidos;
    private String  dni;
    private String  telefono;
    private String  horarioAtencion;
}
