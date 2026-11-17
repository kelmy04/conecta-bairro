package br.com.conectabairro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Um comércio do bairro (Mercado, Barbearia, etc.). */
@Entity @Getter @Setter
public class Comercio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank private String nome;
    @ManyToOne(optional = false) private CategoriaComercio categoria;
    @NotBlank private String endereco;
    @NotBlank private String whatsapp;   // somente dígitos, com DDI
    private String fotoCapa;             // URL da imagem de capa

    // Expediente da AGENDA (só barbearias): o próprio lojista escolhe. O fechamento é o horário em que o
    // atendimento encerra (abre 07:00 e fecha 22:00 -> reservas das 07:00 às 21:50). Nulo nas lojas de produtos.
    private LocalTime abertura;
    private LocalTime fechamento;

    @JsonIgnore @ManyToOne private Usuario dono;

    // Cascata: ao excluir o comércio, o JPA exclui também seus produtos e agendamentos.
    @JsonIgnore
    @OneToMany(mappedBy = "comercio", cascade = CascadeType.REMOVE)
    private List<Produto> produtos = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "comercio", cascade = CascadeType.REMOVE)
    private List<Agendamento> agendamentos = new ArrayList<>();

    /** Barbearias usam agenda; as demais usam catálogo de produtos. */
    @JsonIgnore
    public boolean isAgendavel() {
        return categoria != null
            && categoria.getTipoPainel() == CategoriaComercio.TipoPainel.AGENDAMENTO;
    }

    /** Prestadores de serviços terceirizados (eletricista, encanador...): lista de serviços, sem estoque nem agenda. */
    @JsonIgnore
    public boolean isServico() {
        return categoria != null
            && categoria.getTipoPainel() == CategoriaComercio.TipoPainel.SERVICOS;
    }
}
