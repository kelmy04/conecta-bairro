package br.com.conectabairro.controller;

import br.com.conectabairro.dto.AgendamentoForm;
import br.com.conectabairro.dto.ComercioForm;
import br.com.conectabairro.dto.ConfirmacaoAgendamento;
import br.com.conectabairro.exception.ArquivoInvalidoException;
import br.com.conectabairro.exception.ExpedienteInvalidoException;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.Agendamento;
import br.com.conectabairro.model.Comercio;
import br.com.conectabairro.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;

/** MVC da Vitrine Pública (moradores): lista, detalhe, agendar, login e cadastrar comércio. */
@Controller @RequiredArgsConstructor
public class VitrineController {
    private final ComercioService comercios;
    private final ProdutoService produtos;
    private final AgendamentoService agenda;

    @GetMapping("/")
    public String inicio(Model m) {
        m.addAttribute("grupos", comercios.agruparPorCategoria());   // Map<categoria, List<comércio>>
        return "vitrine";
    }

    @GetMapping("/comercio/{id}")
    public String detalhe(@PathVariable Long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                          Model m) {
        Comercio c = comercios.buscar(id);
        if (c.isAgendavel()) {
            prepararAgendamento(m, c, data != null ? data : LocalDate.now());
            m.addAttribute("agendamentoForm", new AgendamentoForm());
        } else {
            m.addAttribute("c", c);
            m.addAttribute("produtos", produtos.listar(id));
        }
        return "comercio";
    }

    /**
     * ITEM 1: o formulário agora é validado (@Valid + BindingResult). Havendo erro, a PRÓPRIA página do
     * comércio é exibida de novo, com as mensagens em vermelho e os dados já digitados.
     * ITEM 4: dando certo, vai para a tela de sucesso (com o botão "Confirmar pelo WhatsApp").
     */
    @PostMapping("/comercio/{id}/agendar")
    public String agendar(@PathVariable Long id,
                          @Valid @ModelAttribute("agendamentoForm") AgendamentoForm form,
                          BindingResult br, Model m, RedirectAttributes ra) {
        Comercio c = comercios.buscar(id);
        if (!c.isAgendavel()) return "redirect:/comercio/" + id;

        if (!br.hasErrors()) {
            try {
                Agendamento ag = agenda.agendar(id, form.getNome().trim(), form.getTelefone().trim(),
                        form.getServico(), LocalDateTime.of(form.getData(), form.getHora()));
                // "flash": os dados vão para a próxima tela e depois somem (não ficam na URL)
                ra.addFlashAttribute("confirmacao", new ConfirmacaoAgendamento(ag.getClienteNome(), ag.getServico(),
                        ag.getDataHora().toLocalDate(), ag.getDataHora().toLocalTime()));
                return "redirect:/comercio/" + id + "/agendado";
            } catch (RegraNegocioException e) {
                m.addAttribute("erro", e.getMessage());   // ex.: horário já reservado, data distante demais
            }
        }
        prepararAgendamento(m, c, form.getData() != null ? form.getData() : LocalDate.now());
        return "comercio";
    }

    /** ITEM 4: tela de sucesso. Se a página for recarregada (sem os dados do flash), volta ao comércio. */
    @GetMapping("/comercio/{id}/agendado")
    public String agendado(@PathVariable Long id, Model m) {
        if (!m.containsAttribute("confirmacao")) return "redirect:/comercio/" + id;
        m.addAttribute("c", comercios.buscar(id));
        return "agendamento-sucesso";
    }

    /** Tela de login (o envio do formulário é tratado pelo Spring Security). */
    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/cadastrar")
    public String formCadastro(Model m) {
        ComercioForm form = new ComercioForm();
        form.setAbertura(AgendamentoService.ABERTURA_PADRAO);      // sugestão (o lojista escolhe)
        form.setFechamento(AgendamentoService.FECHAMENTO_PADRAO);
        m.addAttribute("comercioForm", form);
        m.addAttribute("editando", false);   // o mesmo formulário também serve para editar
        m.addAttribute("categorias", comercios.listarCategorias());
        return "cadastro";
    }

    /**
     * CORREÇÃO B3: @Valid confere as regras do ComercioForm. Se algo estiver errado, o
     * formulário é exibido de novo, com as mensagens e os dados já digitados (sem erro 500).
     */
    @PostMapping("/cadastrar")
    public String cadastrar(@Valid @ModelAttribute("comercioForm") ComercioForm form,
                            BindingResult br, Authentication auth, Model m) {
        if (!br.hasErrors()) {
            try {
                // ETAPA 1: a foto (MultipartFile do formulário) é salva dentro do service
                Comercio c = comercios.cadastrar(form, auth.getName());
                return "redirect:/painel/" + c.getId();
            } catch (ArquivoInvalidoException e) {
                br.rejectValue("fotoCapa", "foto.invalida", e.getMessage());        // erro no campo da foto
            } catch (ExpedienteInvalidoException e) {
                br.rejectValue("fechamento", "expediente.invalido", e.getMessage());   // erro nos horários
            } catch (RegraNegocioException e) {
                br.rejectValue("categoriaId", "categoria.invalida", e.getMessage());
            }
        }
        m.addAttribute("editando", false);
        m.addAttribute("categorias", comercios.listarCategorias());
        return "cadastro";
    }

    /** Dados que a tela do comércio (barbearia) precisa: dia escolhido, horários livres, serviços e limites de data. */
    private void prepararAgendamento(Model m, Comercio c, LocalDate dia) {
        LocalDate hoje = LocalDate.now();
        m.addAttribute("c", c);
        m.addAttribute("dia", dia);
        m.addAttribute("horarios", agenda.slotsDoDia(c.getId(), dia));   // todos os horários, marcando os reservados
        m.addAttribute("servicos", agenda.servicosDe(c));   // cada categoria tem seus serviços
        m.addAttribute("abertura", agenda.abertura(c));       // expediente escolhido pelo lojista
        m.addAttribute("fechamento", agenda.fechamento(c));
        m.addAttribute("hoje", hoje);
        m.addAttribute("limite", hoje.plusDays(AgendamentoService.DIAS_MAXIMOS));
    }
}
