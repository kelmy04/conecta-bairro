package br.com.conectabairro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** Produto de um comércio (BigDecimal evita erros de arredondamento em dinheiro). */
@Entity @Getter @Setter
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore @ManyToOne(optional = false) private Comercio comercio;

    @NotBlank private String nome;
    @NotNull @DecimalMin("0.01") private BigDecimal preco;
    @NotNull @Min(0) private Integer estoque;
    private String fotoUrl;
}
