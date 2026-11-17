package br.com.conectabairro.repository;

import br.com.conectabairro.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByComercioIdAndDataHoraBetweenOrderByDataHora(
            Long comercioId, LocalDateTime inicio, LocalDateTime fim);

    // Só devolve o agendamento se ele pertencer ao comércio informado (usado ao cancelar)
    Optional<Agendamento> findByIdAndComercioId(Long id, Long comercioId);

    // Usado ao trocar o tipo do comércio: impede esconder clientes que já têm horário marcado
    boolean existsByComercioIdAndStatusAndDataHoraAfter(
            Long comercioId, Agendamento.Status status, LocalDateTime dataHora);

    boolean existsByComercioIdAndDataHoraAndStatus(
            Long comercioId, LocalDateTime dataHora, Agendamento.Status status);
}
