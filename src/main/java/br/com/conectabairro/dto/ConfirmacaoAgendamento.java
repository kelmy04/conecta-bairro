package br.com.conectabairro.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ITEM 4: resumo do agendamento recém-feito, enviado à tela de sucesso por "flash attribute"
 * (vale para um único redirecionamento). Assim a página de sucesso não precisa de um id na URL,
 * e ninguém consegue ver o agendamento de outra pessoa trocando um número no endereço.
 */
@Getter @AllArgsConstructor
public class ConfirmacaoAgendamento implements Serializable {
    private final String nome;
    private final String servico;
    private final LocalDate data;
    private final LocalTime hora;
}
