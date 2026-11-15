package br.com.conectabairro.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Horário marcado por um cliente em uma barbearia. */
@Entity @Getter @Setter
public class Agendamento {
    public enum Status { CONFIRMADO, CANCELADO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Comercio comercio;
    private String clienteNome;
    private String clienteTelefone;
    private String servico;
    private LocalDateTime dataHora;
    @Enumerated(EnumType.STRING) private Status status = Status.CONFIRMADO;
}
