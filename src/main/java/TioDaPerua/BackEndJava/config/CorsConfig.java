package TioDaPerua.BackEndJava.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS para permitir que o FrontEnd (React Native/Expo)
 * consiga consumir a API sem restrições de segurança do navegador.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Habilita CORS para todos os endpoints da API
        registry.addMapping("/**")
                // Origens permitidas (FrontEnd Web, Expo, etc)
                .allowedOrigins(
                    "http://localhost",
                    "http://localhost:8081",
                    "http://localhost:19006",
                    "http://localhost:3000"
                )
                // Métodos HTTP permitidos
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Todos os headers são permitidos
                .allowedHeaders("*")
                // Permite o envio de cookies e autenticação
                .allowCredentials(true)
                // Tempo de cache para preflight requests (1 hora)
                .maxAge(3600);
    }
}
