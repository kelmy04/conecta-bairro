package br.com.conectabairro.repository;

import br.com.conectabairro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Busca o lojista pelo e-mail (usado no login)
    Optional<Usuario> findByEmail(String email);

    // Ignora maiúsculas/minúsculas: usado no login e para impedir e-mails repetidos
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
