package com.smartvet.app.security;

import com.smartvet.app.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class SmartVetUserDetails extends User {

    private final Integer idUsuario;
    private final String nombres;
    private final String apellidos;
    private final String rolNombre;

    public SmartVetUserDetails(Usuario usuario) {
        super(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().getNombre()))
        );
        this.idUsuario = usuario.getIdUsuario();
        this.nombres   = usuario.getNombres();
        this.apellidos = usuario.getApellidos();
        this.rolNombre = usuario.getRol().getNombre();
    }

    public Integer getIdUsuario()      { return idUsuario; }
    public String  getNombreCompleto() { return nombres + " " + apellidos; }
    public String  getRolNombre()      { return rolNombre; }
}
