package br.com.conectabairro.config;

import br.com.conectabairro.repository.ComercioRepository;
import br.com.conectabairro.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * "Porteiro" usado nas anotações @PreAuthorize do painel e da API REST.
 * Responde: o e-mail do usuário logado é o do DONO do comércio (ou do produto) em questão?
 * Se não for, o Spring lança AccessDeniedException e o usuário recebe HTTP 403.
 * (Id inexistente também resulta em 403, para não revelar quais ids existem.)
 */
@Component("guard")   // nome usado nas expressões: @guard.ehDono(...)
@RequiredArgsConstructor
public class DonoGuard {
    private final ComercioRepository comercios;
    private final ProdutoRepository produtos;

    /** O usuário logado é dono do comércio de id informado? */
    public boolean ehDono(Long comercioId, String email) {
        return comercioId != null && email != null && comercios.existsByIdAndDonoEmail(comercioId, email);
    }

    /** O usuário logado é dono do comércio ao qual o produto pertence? */
    public boolean ehDonoDoProduto(Long produtoId, String email) {
        return produtoId != null && email != null && produtos.existsByIdAndComercioDonoEmail(produtoId, email);
    }
}
