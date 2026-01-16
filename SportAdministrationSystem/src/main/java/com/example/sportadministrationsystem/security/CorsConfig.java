package com.example.sportadministrationsystem.security;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    private final Environment env;

    public CorsConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowed = Binder.get(env)
                .bind("app.cors.allowed-origins", Bindable.listOf(String.class))
                .orElse(List.of(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://raspberrypi.local:*",
                        "http://*.local:*",
                        "http://192.168.*.*:*"
                ))
                .stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList();

        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(allowed);
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.addAllowedHeader("*");
        c.setExposedHeaders(List.of("Authorization","Location","Content-Disposition"));
        c.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

}
