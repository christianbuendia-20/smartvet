package com.smartvet.app.service.impl;

import com.smartvet.app.dto.LoginDTO;
import com.smartvet.app.dto.UsuarioRegistroDTO;
import com.smartvet.app.exception.CredencialesInvalidasException;
import com.smartvet.app.exception.EmailDuplicadoException;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Rol;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.model.Veterinario;
import com.smartvet.app.repository.RolRepository;
import com.smartvet.app.repository.UsuarioRepository;
import com.smartvet.app.repository.VeterinarioRepository;
import com.smartvet.app.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final VeterinarioRepository veterinarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                               RolRepository rolRepository,
                               VeterinarioRepository veterinarioRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.veterinarioRepository = veterinarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Usuario registrarCliente(UsuarioRegistroDTO dto) {
        validarEmailUnico(dto.email());

        Rol rol = obtenerRol("cliente");
        Usuario guardado = usuarioRepository.save(construirUsuario(dto, rol));

        log.info("Cliente registrado: id={}, email={}", guardado.getIdUsuario(), guardado.getEmail());
        return guardado;
    }

    @Override
    @Transactional
    public Usuario registrarVeterinario(UsuarioRegistroDTO dto, String horarioAtencion) {
        validarEmailUnico(dto.email());

        Rol rol = obtenerRol("veterinario");
        Usuario guardado = usuarioRepository.save(construirUsuario(dto, rol));

        Veterinario veterinario = new Veterinario();
        veterinario.setUsuario(guardado);
        veterinario.setHorarioAtencion(horarioAtencion);
        veterinarioRepository.save(veterinario);

        log.info("Veterinario registrado: id={}, email={}", guardado.getIdUsuario(), guardado.getEmail());
        return guardado;
    }

    @Override
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con id=" + id + " no encontrado"));
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario con email '" + email + "' no encontrado"));
    }

    @Override
    public List<Usuario> listarClientes() {
        return usuarioRepository.findByRol_Nombre("cliente");
    }

    @Override
    public List<Usuario> listarVeterinarios() {
        return usuarioRepository.findByRol_Nombre("veterinario");
    }

    @Override
    public Usuario login(LoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> {
                    log.warn("Intento de login con email no registrado: {}", dto.email());
                    return new CredencialesInvalidasException();
                });

        if (!usuario.getActivo() || !passwordEncoder.matches(dto.password(), usuario.getPasswordHash())) {
            log.warn("Login fallido para email={}", dto.email());
            throw new CredencialesInvalidasException();
        }

        log.info("Login exitoso: id={}, email={}", usuario.getIdUsuario(), usuario.getEmail());
        return usuario;
    }

    @Override
    @Transactional
    public void desactivarUsuario(Integer id) {
        Usuario usuario = buscarPorId(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado: id={}", id);
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional
    public Usuario cambiarRol(Integer idUsuario, String nuevoRol) {
        Usuario usuario = buscarPorId(idUsuario);
        Rol rol = obtenerRol(nuevoRol);
        usuario.setRol(rol);
        if ("veterinario".equals(nuevoRol) && veterinarioRepository.findByUsuario_IdUsuario(idUsuario).isEmpty()) {
            Veterinario veterinario = new Veterinario();
            veterinario.setUsuario(usuario);
            veterinarioRepository.save(veterinario);
        }
        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Rol de usuario id={} cambiado a '{}'", idUsuario, nuevoRol);
        return actualizado;
    }

    // ── helpers de mapeo y validación ────────────────────────────────────────

    private Usuario construirUsuario(UsuarioRegistroDTO dto, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setEmail(dto.email());
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setNombres(dto.nombres());
        usuario.setApellidos(dto.apellidos());
        usuario.setDni(dto.dni());
        usuario.setTelefono(dto.telefono());
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }

    private void validarEmailUnico(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailDuplicadoException(email);
        }
    }

    private Rol obtenerRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol '" + nombre + "' no encontrado en la base de datos"));
    }
}
