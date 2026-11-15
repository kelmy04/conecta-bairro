package br.com.conectabairro.repository;

import br.com.conectabairro.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    // O Spring Data gera o SQL a partir do nome do método
    List<Produto> findByComercioIdOrderByNome(Long comercioId);

    // Busca o produto SOMENTE se ele pertencer ao comércio informado (evita apagar produto de outro comércio)
    Optional<Produto> findByIdAndComercioId(Long id, Long comercioId);

    // Existe produto com este id cujo comércio pertence a este e-mail? (usado pelo DonoGuard na API)
    boolean existsByIdAndComercioDonoEmail(Long id, String email);
}
