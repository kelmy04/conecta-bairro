package br.com.conectabairro.repository;

import br.com.conectabairro.model.Comercio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {
    // ETAPA 2: comércios de um lojista (o Spring Data lê "Dono" + "Email" e gera o SQL: dono.email = ?)
    List<Comercio> findByDonoEmail(String email);

    // ETAPA 4: usado pelo DonoGuard para saber se o e-mail logado é o dono do comércio
    boolean existsByIdAndDonoEmail(Long id, String email);
}
