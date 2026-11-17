package br.com.conectabairro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Categoria do comércio. O tipoPainel decide qual painel o lojista enxerga. */
@Entity @Getter @Setter
public class CategoriaComercio {
    // PRODUTOS = catálogo/cardápio | AGENDAMENTO = agenda de horários | SERVICOS = prestadores de serviço (lista de serviços e preços, orçamento pelo WhatsApp)
    public enum TipoPainel { PRODUTOS, AGENDAMENTO, SERVICOS }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TipoPainel tipoPainel;

    // Serviços que o cliente pode escolher ao agendar, separados por ponto e vírgula
    // (ex.: "Corte;Barba;Corte + Barba"). Vazio = usa a lista padrão de barbearia.
    @Column(length = 300)
    private String servicos;
}
