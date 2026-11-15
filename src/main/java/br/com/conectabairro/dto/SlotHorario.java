package br.com.conectabairro.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalTime;

/**
 * Um horário da agenda do dia e se ele já está reservado.
 * A tela mostra TODOS os horários: os reservados aparecem marcados e, ao escolhê-los,
 * o cliente recebe o aviso de que o horário já foi ocupado por outra pessoa.
 */
@Getter @AllArgsConstructor
public class SlotHorario {
    private final LocalTime hora;
    private final boolean ocupado;
}
