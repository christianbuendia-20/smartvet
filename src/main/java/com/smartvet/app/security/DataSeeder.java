package com.smartvet.app.security;

import com.smartvet.app.model.Rol;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.repository.RolRepository;
import com.smartvet.app.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      RolRepository rolRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository     = rolRepository;
        this.passwordEncoder   = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.existsByEmail("admin@smartvet.com")) {
            log.info("DataSeeder: usuario admin ya existe, omitiendo seed.");
            return;
        }

        Rol rolAdmin = rolRepository.findByNombre("admin")
                .orElseGet(() -> {
                    Rol r = new Rol();
                    r.setNombre("admin");
                    log.info("DataSeeder: rol 'admin' creado.");
                    return rolRepository.save(r);
                });

        Usuario admin = new Usuario();
        admin.setEmail("admin@smartvet.com");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setNombres("Administrador");
        admin.setApellidos("SmartVet");
        admin.setRol(rolAdmin);
        admin.setActivo(true);

        usuarioRepository.save(admin);
        log.info("DataSeeder: usuario admin@smartvet.com creado correctamente.");
    }
}
