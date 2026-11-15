package br.com.conectabairro.repository;

import br.com.conectabairro.model.Imagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImagemRepository extends JpaRepository<Imagem, String> {

    /**
     * Apaga pelo nome SEM carregar os bytes da foto antes (um deleteById leria a imagem inteira
     * só para depois apagá-la). Precisa rodar dentro de uma transação.
     */
    @Modifying
    @Query("delete from Imagem i where i.nome = :nome")
    int apagarPorNome(@Param("nome") String nome);
}
