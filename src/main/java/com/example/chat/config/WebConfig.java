package com.example.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração para CORS e segurança
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
<<<<<<< Updated upstream
    @Value("${chat.allowed-origins}")
    private String allowedOrigins;
=======
    @org.springframework.beans.factory.annotation.Value("${chat.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;
>>>>>>> Stashed changes

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
<<<<<<< Updated upstream
                .allowedOriginPatterns(allowedOrigins.split(","))
=======
                .allowedOriginPatterns(allowedOrigins)
>>>>>>> Stashed changes
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}




