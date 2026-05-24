package com.smartvet.app.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/auth/login";
        }

        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> {
                    String rol = a.getAuthority().toLowerCase().replace("role_", "");
                    return switch (rol) {
                        case "admin"       -> "redirect:/admin/dashboard";
                        case "veterinario" -> "redirect:/vet/dashboard";
                        default            -> "redirect:/cliente/dashboard";
                    };
                })
                .orElseGet(() -> {
                    log.warn("Usuario autenticado sin autoridades: {}", auth.getName());
                    return "redirect:/auth/login";
                });
    }
}
