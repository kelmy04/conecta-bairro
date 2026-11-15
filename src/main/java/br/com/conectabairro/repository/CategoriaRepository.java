package br.com.conectabairro.repository;

import br.com.conectabairro.model.CategoriaComercio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<CategoriaComercio, Long> {
    Optional<CategoriaComercio> findByNome(String nome);
}
