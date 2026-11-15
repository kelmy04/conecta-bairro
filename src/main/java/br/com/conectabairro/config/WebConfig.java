package br.com.conectabairro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Expõe a pasta "uploads/" (do disco) na rota pública /uploads/**, para o navegador
 * conseguir exibir as imagens. Ex.: arquivo uploads/abc.jpg  ->  http://localhost:8080/uploads/abc.jpg
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:uploads}")
    private String pastaUploads;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String local = Path.of(pastaUploads).toAbsolutePath().normalize().toUri().toString();
        if (!local.endsWith("/")) local += "/";   // o Spring exige a barra final em pastas
        registro.addResourceHandler("/uploads/**")
                .addResourceLocations(local)
                // os nomes são UUIDs únicos, então o navegador pode guardar a imagem em cache por bastante tempo
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
