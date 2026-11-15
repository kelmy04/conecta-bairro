package br.com.conectabairro.service;

import br.com.conectabairro.dto.SlotHorario;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.Agendamento;
import br.com.conectabairro.model.Agendamento.Status;
import br.com.conectabairro.model.Comercio;
import br.com.conectabairro.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Lógica de agendamento da barbearia: valida e reserva horários. */
@Service @RequiredArgsConstructor
public class AgendamentoService {
    // Valores usados só quando o comércio ainda não tem expediente definido (cadastros antigos)
    public static final LocalTime ABERTURA_PADRAO = LocalTime.of(8, 0);
    public static final LocalTime FECHAMENTO_PADRAO = LocalTime.of(18, 0);
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    public static final int DIAS_MAXIMOS = 60;      // ITEM 1: antecedência máxima para agendar
    // Serviços usados quando a categoria não define os seus (barbearia)
    public static final List<String> SERVICOS_PADRAO = List.of("Corte", "Barba", "Corte + Barba");
    public static final int DURACAO_MIN = 10;          // horários de 10 em 10 minutos

    private final AgendamentoRepository repo;
    private final ComercioService comercios;

    /** Serviços oferecidos por este comércio, conforme a sua categoria (barbearia, cabeleireiro(a)...). */
    public List<String> servicosDe(Comercio c) {
        String bruto = c.getCategoria().getServicos();
        if (bruto == null || bruto.isBlank()) return SERVICOS_PADRAO;
        return Arrays.stream(bruto.split(";")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    /** Horário de abertura da agenda deste comércio (escolhido pelo lojista). */
    public LocalTime abertura(Comercio c) {
        return c.getAbertura() != null ? c.getAbertura() : ABERTURA_PADRAO;
    }

    /** Horário de fechamento: o atendimento encerra aqui (o último horário é DURACAO_MIN minutos antes). */
    public LocalTime fechamento(Comercio c) {
        return c.getFechamento() != null ? c.getFechamento() : FECHAMENTO_PADRAO;
    }

    /** Agenda do dia (List) para o lojista. */
    public List<Agendamento> listarDoDia(Long comercioId, LocalDate dia) {
        return repo.findByComercioIdAndDataHoraBetweenOrderByDataHora(
                comercioId, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    /**
     * Todos os horários do dia (de 10 em 10 minutos, dentro do expediente) indicando se cada um já está
     * RESERVADO. Horários que já passaram (hoje) não aparecem. Datas passadas ou além do limite de
     * antecedência não têm horários.
     */
    public List<SlotHorario> slotsDoDia(Long comercioId, LocalDate dia) {
        if (dia.isBefore(LocalDate.now()) || dia.isAfter(LocalDate.now().plusDays(DIAS_MAXIMOS)))
            return List.of();
        Comercio c = comercios.buscar(comercioId);   // o expediente (abertura/fechamento) é de cada comércio
        Set<LocalTime> ocupados = listarDoDia(comercioId, dia).stream()
                .filter(a -> a.getStatus() == Status.CONFIRMADO)
                .map(a -> a.getDataHora().toLocalTime())
                .collect(Collectors.toSet());
        List<SlotHorario> slots = new ArrayList<>();
        for (LocalTime h = abertura(c); h.isBefore(fechamento(c)); h = h.plusMinutes(DURACAO_MIN)) {
            boolean jaPassou = dia.equals(LocalDate.now()) && h.isBefore(LocalTime.now());
            if (!jaPassou) slots.add(new SlotHorario(h, ocupados.contains(h)));
        }
        return slots;
    }

    /** Somente os horários livres (mesma regra de slotsDoDia, sem os reservados). */
    public List<LocalTime> horariosLivres(Long comercioId, LocalDate dia) {
        return slotsDoDia(comercioId, dia).stream()
                .filter(s -> !s.isOcupado())
                .map(SlotHorario::getHora)
                .toList();
    }

    /**
     * VALIDAÇÃO DO HORÁRIO: 1) é barbearia; 2) não está no passado; 3) dentro do
     * expediente do comércio e alinhado aos horários de 10 em 10 min; 4) ninguém reservou o mesmo horário.
     * @Transactional + índice único no banco garantem que 2 clientes simultâneos
     * não reservem o mesmo horário (o segundo recebe erro).
     */
    @Transactional
    public Agendamento agendar(Long comercioId, String nome, String telefone,
                               String servico, LocalDateTime dataHora) {
        Comercio c = comercios.buscar(comercioId);
        if (!c.isAgendavel())
            throw new RegraNegocioException("Este comércio não trabalha com agendamento.");
        if (dataHora.isBefore(LocalDateTime.now()))
            throw new RegraNegocioException("Escolha um horário que ainda não passou.");
        // ITEM 1: não permite datas muito distantes (evita reservas "de mentira" para daqui a anos)
        if (dataHora.isAfter(LocalDateTime.now().plusDays(DIAS_MAXIMOS)))
            throw new RegraNegocioException("Só é possível agendar com até " + DIAS_MAXIMOS + " dias de antecedência.");
        // O serviço vem de um <select>, mas o servidor também confere (o formulário pode ser forjado)
        if (!servicosDe(c).contains(servico))
            throw new RegraNegocioException("Escolha um serviço da lista.");

        LocalTime h = dataHora.toLocalTime();
        boolean foraDoExpediente = h.isBefore(abertura(c)) || !h.isBefore(fechamento(c));
        boolean desalinhado = h.getMinute() % DURACAO_MIN != 0;
        if (foraDoExpediente || desalinhado)
            throw new RegraNegocioException("O atendimento é das " + abertura(c).format(HHMM) + " às " + fechamento(c).format(HHMM)
                    + " (último horário às " + fechamento(c).minusMinutes(DURACAO_MIN).format(HHMM) + "), de "
                    + DURACAO_MIN + " em " + DURACAO_MIN + " minutos.");

        if (repo.existsByComercioIdAndDataHoraAndStatus(comercioId, dataHora, Status.CONFIRMADO))
            throw new RegraNegocioException("Esse horário já foi reservado. Escolha outro.");

        Agendamento a = new Agendamento();
        a.setComercio(c);
        a.setClienteNome(nome);
        a.setClienteTelefone(telefone);
        a.setServico(servico);
        a.setDataHora(dataHora);
        // CORREÇÃO B1: se dois clientes reservarem o mesmo horário ao mesmo tempo, os dois passam
        // pela checagem acima, mas o índice único do banco (uq_agendamento_horario) barra o segundo.
        // Capturamos esse erro do banco e o transformamos em uma mensagem amigável (antes: erro 500).
        // saveAndFlush força o INSERT imediatamente, garantindo que o erro caia dentro deste try.
        try {
            return repo.saveAndFlush(a);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Esse horário acabou de ser reservado. Escolha outro.");
        }
    }

    /**
     * ITEM 2: cancela um agendamento (o status vira CANCELADO; o registro fica no histórico).
     * Só encontra agendamentos DESTE comércio (findByIdAndComercioId), e a rota do painel já
     * exige que o usuário logado seja o dono (@PreAuthorize no PainelController).
     * O índice único do banco só vale para CONFIRMADO, então o horário volta a ficar livre.
     */
    @Transactional
    public Agendamento cancelar(Long comercioId, Long agendamentoId) {
        Agendamento a = repo.findByIdAndComercioId(agendamentoId, comercioId)
                .orElseThrow(() -> new RegraNegocioException("Agendamento não encontrado."));
        if (a.getStatus() == Status.CANCELADO)
            throw new RegraNegocioException("Este agendamento já foi cancelado.");
        a.setStatus(Status.CANCELADO);
        return a;
    }
}
