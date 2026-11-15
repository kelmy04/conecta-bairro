package br.com.conectabairro.controller;

import br.com.conectabairro.dto.ProdutoForm;
import br.com.conectabairro.exception.ArquivoInvalidoException;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.*;
import br.com.conectabairro.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

/**
 * MVC do Painel do Lojista. O mesmo endereço mostra painel de produtos OU de agenda.
 *
 * ETAPA 4 (correção do IDOR): o @PreAuthorize abaixo vale para TODOS os métodos da classe.
 * Antes de executar qualquer um, o Spring chama DonoGuard.ehDono(id, e-mail do logado). Se o
 * usuário não for o dono do comércio {id}, o acesso é barrado com HTTP 403 (Forbidden).
 */
@Controller @RequestMapping("/painel/{id}") @RequiredArgsConstructor
@PreAuthorize("@guard.ehDono(#id, authentication.name)")
public class PainelController {
    private final ComercioService comercios;
    private final ProdutoService produtos;
    private final AgendamentoService agenda;

    @GetMapping
    public String painel(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         Model m) {
        Comercio c = comercios.buscar(id);
        m.addAttribute("c", c);
        if (c.isAgendavel()) {
            LocalDate dia = data != null ? data : LocalDate.now();
            m.addAttribute("dia", dia);
            m.addAttribute("agenda", agenda.listarDoDia(id, dia));     // List de agendamentos do dia
        } else {
            m.addAttribute("produtos", produtos.listar(id));           // List de produtos
        }
        return "painel";
    }

    /**
     * ETAPA 1: recebe o formulário (multipart) com a foto do produto. O ProdutoForm valida os
     * campos; o ProdutoService salva a imagem em uploads/ e grava o caminho na coluna foto_url.
     */
    @PostMapping("/produtos")
    public String novoProduto(@PathVariable Long id, @Valid @ModelAttribute("produtoForm") ProdutoForm form,
                              BindingResult br, RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("erro", primeiroErro(br));
        } else {
            try {
                produtos.cadastrar(id, form);
                ra.addFlashAttribute("ok", "Produto salvo!");
            } catch (RegraNegocioException e) {
                ra.addFlashAttribute("erro", e.getMessage());   // ex.: imagem inválida ou grande demais
            }
        }
        return "redirect:/painel/" + id;
    }

    // ---------- ITEM 3: editar produto ----------
    @GetMapping("/produtos/{pid}/editar")
    public String formEditarProduto(@PathVariable Long id, @PathVariable Long pid, Model m) {
        Produto p = produtos.buscarDoComercio(id, pid);        // só produtos DESTE comércio
        ProdutoForm form = new ProdutoForm();                   // formulário já preenchido
        form.setNome(p.getNome());
        form.setPreco(p.getPreco());
        form.setEstoque(p.getEstoque());
        m.addAttribute("produtoForm", form);
        prepararEdicao(m, id, p);
        return "editar-produto";
    }

    @PostMapping("/produtos/{pid}/editar")
    public String salvarProduto(@PathVariable Long id, @PathVariable Long pid,
                                @Valid @ModelAttribute("produtoForm") ProdutoForm form,
                                BindingResult br, Model m, RedirectAttributes ra) {
        Produto p = produtos.buscarDoComercio(id, pid);
        if (!br.hasErrors()) {
            try {
                produtos.editar(id, pid, form);
                ra.addFlashAttribute("ok", "Produto atualizado!");
                return "redirect:/painel/" + id;
            } catch (ArquivoInvalidoException e) {
                br.rejectValue("fotoProduto", "foto.invalida", e.getMessage());   // erro junto ao campo da foto
            }
        }
        prepararEdicao(m, id, p);   // volta ao formulário com as mensagens de erro
        return "editar-produto";
    }

    // ---------- ITEM 2: cancelar agendamento ----------
    /** O @PreAuthorize da classe garante que só o DONO do comércio {id} chega até aqui. */
    @PostMapping("/agendamentos/{agId}/cancelar")
    public String cancelarAgendamento(@PathVariable Long id, @PathVariable Long agId, RedirectAttributes ra) {
        try {
            Agendamento ag = agenda.cancelar(id, agId);
            ra.addFlashAttribute("ok", "Agendamento cancelado. O horário voltou a ficar livre.");
            return "redirect:/painel/" + id + "?data=" + ag.getDataHora().toLocalDate();   // volta ao mesmo dia
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/painel/" + id;
        }
    }

    @PostMapping("/produtos/{pid}/excluir")
    public String excluir(@PathVariable Long id, @PathVariable Long pid, RedirectAttributes ra) {
        produtos.excluir(id, pid);   // só apaga se o produto for deste comércio
        ra.addFlashAttribute("ok", "Produto removido.");
        return "redirect:/painel/" + id;
    }

    /** Mensagem amigável: erro de conversão (ex.: letras no preço) vira um aviso genérico. */
    private String primeiroErro(BindingResult br) {
        FieldError e = br.getFieldError();
        if (e == null || e.isBindingFailure()) return "Confira o nome, o preço e o estoque informados.";
        return e.getDefaultMessage();
    }

    /** Dados extras usados pelo template editar-produto.html. */
    private void prepararEdicao(Model m, Long id, Produto p) {
        m.addAttribute("c", comercios.buscar(id));
        m.addAttribute("produto", p);   // foto atual e links
    }
}
