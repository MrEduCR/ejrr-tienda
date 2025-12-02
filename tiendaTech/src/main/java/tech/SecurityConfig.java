package tech;

import tech.domain.Ruta;
import tech.services.RutaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {   

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, @Lazy RutaService rutaService) throws Exception {
        var rutas = rutaService.getRutas();

        http.authorizeHttpRequests(requests -> {
            // PERMITIR RECURSOS ESTÁTICOS
            requests.requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/fav/**"
            ).permitAll();

            // RUTAS DINÁMICAS DESDE BD
            for (Ruta ruta : rutas) {
                if (ruta.isRequiereRol()) {
                    requests.requestMatchers(ruta.getRuta()).hasRole(ruta.getRol().getRol());
                } else {
                    requests.requestMatchers(ruta.getRuta()).permitAll();
                }
            }

            // CUALQUIER OTRA RUTA → LOGIN
            requests.anyRequest().authenticated();
        });

        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
        ).logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        ).exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/acceso_denegado")
        ).sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
        );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configurerGlobal(AuthenticationManagerBuilder build, 
            @Lazy PasswordEncoder passwordEncoder, 
            @Lazy UserDetailsService userDetailsService) throws Exception {
        build.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

}