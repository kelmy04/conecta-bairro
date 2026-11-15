package br.com.conectabairro.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/** Lojista dono de um comércio. (Login com Spring Security: próxima etapa.) */
@Entity @Getter @Setter
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @Column(unique = true) private String email;
    @JsonIgnore private String senhaHash;   // nunca sai na API

    /**
     * ETAPA 2 (exclusão de conta): ao excluir o usuário, o JPA exclui em CASCATA todos os seus
     * comércios (e, por consequência, os produtos e agendamentos de cada um).
     */
    @JsonIgnore
    @OneToMany(mappedBy = "dono", cascade = CascadeType.REMOVE)
    private List<Comercio> comercios = new ArrayList<>();
}
