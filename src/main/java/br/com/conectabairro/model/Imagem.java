package br.com.conectabairro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Foto enviada por um lojista, guardada NO BANCO (tabela "imagem").
 *
 * Por que no banco e não numa pasta? Hospedagens gratuitas apagam o disco do site a cada nova
 * publicação ou reinício. O banco é permanente (e já tem backup/cópia na hospedagem do banco),
 * então as fotos nunca somem. As demais tabelas continuam guardando só o CAMINHO público
 * (ex.: /uploads/3f2a...jpg), como antes: nada mais no sistema precisou mudar.
 */
@Entity @Table(name = "imagem")
@Getter @Setter @NoArgsConstructor
public class Imagem {

    /** Nome aleatório (UUID + extensão), gerado pelo sistema; é a parte final do caminho público. */
    @Id @Column(length = 60)
    private String nome;

    /** image/jpeg ou image/png (definido pelo sistema, nunca pelo que o usuário informou). */
    @Column(nullable = false, length = 30)
    private String tipo;

    /** Os bytes da imagem (coluna bytea do PostgreSQL). Já vêm redimensionados e sem EXIF. */
    @Column(nullable = false)
    private byte[] dados;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Imagem(String nome, String tipo, byte[] dados) {
        this.nome = nome;
        this.tipo = tipo;
        this.dados = dados;
    }
}
