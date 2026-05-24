package com.smartvet.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import java.io.IOException;

@Slf4j
public class CustomAuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public CustomAuthSuccessHandler() {
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

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

        log.info("Login exitoso: usuario_id={}, rol={}", details.getIdUsuario(), details.getRolNombre());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
