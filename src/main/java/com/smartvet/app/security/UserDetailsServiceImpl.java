package com.smartvet.app.security;

import com.smartvet.app.model.Usuario;
import com.smartvet.app.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Intento de login con email no registrado: {}", email);
                    return new UsernameNotFoundException("Credenciales inválidas");
                });

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Intento de login con cuenta inactiva: {}", email);
            throw new UsernameNotFoundException("Cuenta inactiva");
        }

        return new SmartVetUserDetails(usuario);
    }
}
