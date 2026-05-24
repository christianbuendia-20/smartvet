package com.smartvet.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        SmartVetUserDetails details = (SmartVetUserDetails) authentication.getPrincipal();

        HttpSession session = request.getSession();
        session.setAttribute("usuarioId",     details.getIdUsuario());
        session.setAttribute("usuarioNombre", details.getNombreCompleto());
        session.setAttribute("usuarioRol",    details.getRolNombre());

        String destino = resolverDestino(authentication);
        log.info("Login exitoso: usuario_id={}, rol={}, destino={}",
                details.getIdUsuario(), details.getRolNombre(), destino);

        response.sendRedirect(request.getContextPath() + destino);
    }

    private String resolverDestino(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(rol -> {
                    String normalizado = rol.toLowerCase().replace("role_", "");
                    return switch (normalizado) {
                        case "admin"       -> "/admin/dashboard";
                        case "veterinario" -> "/vet/dashboard";
                        case "cliente"     -> "/cliente/dashboard";
                        default            -> "/dashboard";
                    };
                })
                .orElse("/dashboard");
    }
}
