package com.smartvet.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**").permitAll()
                        .requestMatchers("/", "/auth/login", "/auth/registro", "/auth/verificar").permitAll()
                        .requestMatchers("/recuperar-password", "/verificar-recuperacion", "/restablecer-password").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("admin")
                        .requestMatchers("/vet/**").hasAuthority("veterinario")
                        .requestMatchers("/cliente/**").hasAuthority("cliente")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth != null) {
                                boolean isAdmin = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("admin"));
                                boolean isVet = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("veterinario"));
                                boolean isCliente = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("cliente"));

                                if (isAdmin) {
                                    response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                                } else if (isVet) {
                                    response.sendRedirect(request.getContextPath() + "/vet/dashboard");
                                } else if (isCliente) {
                                    response.sendRedirect(request.getContextPath() + "/cliente/panel");
                                } else {
                                    response.sendRedirect(request.getContextPath() + "/");
                                }
                            } else {
                                response.sendRedirect(request.getContextPath() + "/auth/login");
                            }
                        })
                );

        return http.build();
    }
}
