package com.smartvet.app.security;

import com.smartvet.app.exception.AccesoDenegadoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Mascota;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class SecurityUtils {

    // ── Consulta del principal activo ─────────────────────────────────────────

    public Optional<SmartVetUserDetails> getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SmartVetUserDetails details) {
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public boolean esCliente() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("cliente"));
    }

    // ── Validaciones de propiedad ─────────────────────────────────────────────

    /**
     * Si el usuario autenticado es cliente, verifica que la mascota le pertenece.
     * Vets y admins siempre pasan esta validación.
     */
    public void verificarPropiedadMascota(Mascota mascota) {
        if (!esCliente()) return;

        getPrincipal().ifPresent(details -> {
            Integer idPropietario = mascota.getPropietario().getIdUsuario();
            if (!idPropietario.equals(details.getIdUsuario())) {
                log.warn("Acceso denegado: cliente usuario_id={} intentó acceder a mascota_id={} " +
                         "perteneciente al propietario_id={}",
                        details.getIdUsuario(), mascota.getIdMascota(), idPropietario);
                throw new AccesoDenegadoException(
                        "No tienes permiso para acceder a la mascota \"" + mascota.getNombre() + "\".");
            }
        });
    }

    /**
     * Si el usuario autenticado es cliente, verifica que la cita pertenece a una de sus mascotas.
     * Vets y admins siempre pasan esta validación.
     */
    public void verificarPropiedadCita(Cita cita) {
        if (!esCliente()) return;

        getPrincipal().ifPresent(details -> {
            Integer idPropietario = cita.getMascota().getPropietario().getIdUsuario();
            if (!idPropietario.equals(details.getIdUsuario())) {
                log.warn("Acceso denegado: cliente usuario_id={} intentó modificar cita_id={} " +
                         "perteneciente al propietario_id={}",
                        details.getIdUsuario(), cita.getIdCita(), idPropietario);
                throw new AccesoDenegadoException(
                        "No tienes permiso para modificar la cita con id=" + cita.getIdCita() + ".");
            }
        });
    }
}
