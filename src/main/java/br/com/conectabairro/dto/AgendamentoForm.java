package br.com.conectabairro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ITEM 1: DTO do formulário de agendamento (tela do comércio).
 * Antes, nome e telefone chegavam como parâmetros soltos, aceitando texto em branco ou gigante.
 * Agora tudo é validado antes de chegar ao service. Os limites de tamanho seguem as colunas do
 * banco (cliente_nome = 100 e cliente_telefone = 20 caracteres).
 */
@Getter @Setter
public class AgendamentoForm {

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    // Aceita máscara, ex.: (65) 99999-0000. Exige de 10 a 15 dígitos no total.
    @NotBlank(message = "Informe seu telefone com DDD.")
    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
    @Pattern(regexp = "\\D*(\\d\\D*){10,15}", message = "Telefone inválido: informe DDD e número (10 a 15 dígitos).")
    private String telefone;

    @NotBlank(message = "Escolha um serviço.")
    @Size(max = 60, message = "Serviço inválido.")
    private String servico;

    @NotNull(message = "Escolha o dia.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull(message = "Escolha um horário.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime hora;
}
