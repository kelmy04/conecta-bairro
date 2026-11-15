package br.com.conectabairro.controller;

import br.com.conectabairro.repository.ImagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.WebRequest;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Entrega as fotos guardadas no banco em /uploads/{nome} (rota pública, liberada no SecurityConfig).
 * Substitui a antiga leitura da pasta "uploads/" do disco: as páginas continuam usando o mesmo
 * endereço (ex.: <img src="/uploads/abc.jpg">), então nenhum template mudou.
 */
@Controller @RequiredArgsConstructor
public class ImagemController {
    private final ImagemRepository imagens;

    /** Só aceitamos o formato que NÓS geramos (UUID + .jpg/.png): barra qualquer outro nome logo na entrada. */
    private static final Pattern NOME_VALIDO = Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|png)");

    @GetMapping("/uploads/{nome}")
    public ResponseEntity<byte[]> ver(@PathVariable String nome, WebRequest request) {
        if (!NOME_VALIDO.matcher(nome).matches()) return ResponseEntity.notFound().build();

        // O nome é um UUID que nunca muda de conteúdo, então serve como identificador de versão (ETag).
        // Se o navegador já tem a foto, responde "304 não modificado" SEM consultar o banco
        // (importante: poupa o banco gratuito e deixa a vitrine rápida). Quando não é o caso,
        // este mesmo método já coloca o cabeçalho ETag na resposta.
        if (request.checkNotModified(nome)) return null;

        return imagens.findById(nome)
                .map(img -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(img.getTipo()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
                        .body(img.getDados()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
