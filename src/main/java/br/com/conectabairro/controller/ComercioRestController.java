package br.com.conectabairro.controller;

import br.com.conectabairro.model.Comercio;
import br.com.conectabairro.service.ComercioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * API REST - CRUD de Comércios.
 *  - Consultas (GET): públicas.
 *  - Criar: exige login; o dono é o usuário logado.
 *  - Alterar e excluir: exigem login E que o usuário seja o DONO do comércio
 *    (@PreAuthorize chama o DonoGuard; se não for o dono, a resposta é HTTP 403).
 * Ex.: POST body {"nome":"..","categoria":{"id":1},"endereco":"..","whatsapp":".."}
 */
@RestController @RequestMapping("/api/comercios") @RequiredArgsConstructor
public class ComercioRestController {
    private final ComercioService service;

    @GetMapping            public List<Comercio> listar()           { return service.listar(); }
    @GetMapping("/{id}")   public Comercio buscar(@PathVariable Long id) { return service.buscar(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Comercio criar(@Valid @RequestBody Comercio c, Authentication auth) {
        return service.criarPelaApi(c, auth.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@guard.ehDono(#id, authentication.name)")
    public Comercio atualizar(@PathVariable Long id, @Valid @RequestBody Comercio c) {
        return service.atualizar(id, c);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@guard.ehDono(#id, authentication.name)")
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}
