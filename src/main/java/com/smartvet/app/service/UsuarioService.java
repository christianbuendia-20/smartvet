package com.smartvet.app.service;

import com.smartvet.app.dto.LoginDTO;
import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.model.Usuario;

import java.util.List;

public interface UsuarioService {

    Usuario registrarCliente(UsuarioRegistroDTO dto);

    Usuario registrarVeterinario(UsuarioRegistroDTO dto, String horarioAtencion);

    Usuario buscarPorId(Integer id);

    Usuario buscarPorEmail(String email);

    List<Usuario> listarClientes();

    List<Usuario> listarVeterinarios();

    Usuario login(LoginDTO dto);

    void desactivarUsuario(Integer id);

    List<Usuario> listarTodos();

    Usuario cambiarRol(Integer idUsuario, String nuevoRol);
}
