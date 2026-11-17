package br.com.conectabairro.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

/**
 * DTO do formulário "Novo produto" do painel. Só aceita os campos abaixo (o "id" e o
 * comércio nunca vêm do formulário), evitando que alguém sobrescreva outro produto.
 */
@Getter @Setter
public class ProdutoForm {

    @NotBlank(message = "Informe o nome do produto.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Preço inválido (use no máximo 2 casas decimais).")
    private BigDecimal preco;

    @NotNull(message = "Informe o estoque.")
    @Min(value = 0, message = "O estoque não pode ser negativo.")
    @Max(value = 100000, message = "Estoque acima do limite permitido.")
    private Integer estoque;

    @Size(max = 300, message = "A descrição deve ter no máximo 300 caracteres.")
    private String descricao;

    // ETAPA 1: foto do produto como arquivo (JPG/PNG até 3 MB, opcional)
    private MultipartFile fotoProduto;
}
