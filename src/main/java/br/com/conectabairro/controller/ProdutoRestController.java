package br.com.conectabairro.controller;

import br.com.conectabairro.model.Produto;
import br.com.conectabairro.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * API REST - CRUD de Produtos (sempre dentro de um comércio).
 *  - Consultas (GET): públicas.
 *  - Criar: só o dono do comércio {cid}.
 *  - Alterar e excluir: só o dono do comércio ao qual o produto pertence.
 *  Quem não for o dono recebe HTTP 403 (verificação feita pelo DonoGuard).
 */
@RestController @RequiredArgsConstructor
public class ProdutoRestController {
    private final ProdutoService service;

    @GetMapping("/api/comercios/{cid}/produtos")
    public List<Produto> listar(@PathVariable Long cid) { return service.listar(cid); }

    @PostMapping("/api/comercios/{cid}/produtos") @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@guard.ehDono(#cid, authentication.name)")
    public Produto criar(@PathVariable Long cid, @Valid @RequestBody Produto p) { return service.salvar(cid, p); }

    @PutMapping("/api/produtos/{id}")
    @PreAuthorize("@guard.ehDonoDoProduto(#id, authentication.name)")
    public Produto atualizar(@PathVariable Long id, @Valid @RequestBody Produto p) { return service.atualizar(id, p); }

    @DeleteMapping("/api/produtos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@guard.ehDonoDoProduto(#id, authentication.name)")
    public void excluir(@PathVariable Long id) { service.excluir(id); }

    /** Texto do post de divulgação (público: é o mesmo texto que o lojista divulgaria). */
    @GetMapping(value = "/api/produtos/{id}/post", produces = "text/plain;charset=UTF-8")
    public String post(@PathVariable Long id) { return service.gerarPost(id); }
}
