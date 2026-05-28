package com.smartvet.app.service;

import com.smartvet.app.dto.LoginDTO;
import com.smartvet.app.dto.PerfilUpdateDTO;
import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.model.DireccionUsuario;
import com.smartvet.app.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    Usuario registrarCliente(UsuarioRegistroDTO dto);

    /** Registra el cliente con activo=false, pendiente de verificación por email. */
    Usuario registrarClientePendiente(UsuarioRegistroDTO dto);

    /** Elimina un usuario que quedó en estado pendiente por fallo en el envío. */
    void eliminarUsuarioPendiente(Integer id);

    Usuario registrarVeterinario(UsuarioRegistroDTO dto, String horarioAtencion);

    Usuario registrarConRol(UsuarioRegistroDTO dto, String rolNombre);

    Usuario actualizarPerfil(Integer idUsuario, PerfilUpdateDTO dto);

    Optional<DireccionUsuario> buscarDireccion(Integer idUsuario);

    Usuario buscarPorId(Integer id);

    Usuario buscarPorEmail(String email);

    List<Usuario> listarClientes();

    List<Usuario> listarVeterinarios();

    Usuario login(LoginDTO dto);

    void desactivarUsuario(Integer id);

    void activarUsuario(Integer id);

    List<Usuario> listarTodos();

    List<Usuario> listarTodos(String keyword);

    Usuario cambiarRol(Integer idUsuario, String nuevoRol);

    void restablecerContrasena(Integer idUsuario, String nuevaContrasena);
}
