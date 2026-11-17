package br.com.conectabairro.controller;

import br.com.conectabairro.dto.ComercioForm;
import br.com.conectabairro.exception.ArquivoInvalidoException;
import br.com.conectabairro.exception.ExpedienteInvalidoException;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.model.Comercio;
import br.com.conectabairro.service.AgendamentoService;
import br.com.conectabairro.service.ComercioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ações do DONO sobre o próprio comércio: editar e excluir.
 * Toda ação confere se o usuário logado é o dono (senão: HTTP 403).
 */
@Controller @RequestMapping("/comercio/{id}") @RequiredArgsConstructor
public class ComercioController {
    private final ComercioService comercios;

    @GetMapping("/editar")
    public String formEditar(@PathVariable Long id, Authentication auth, Model m) {
        Comercio c = comercios.buscarDoDono(id, auth.getName());   // 403 se não for o dono
        ComercioForm form = new ComercioForm();                     // preenche o formulário com os dados atuais
        form.setNome(c.getNome());
        form.setCategoriaId(c.getCategoria().getId());
        form.setEndereco(c.getEndereco());
        form.setWhatsapp(c.getWhatsapp());
        form.setAbertura(c.getAbertura() != null ? c.getAbertura() : AgendamentoService.ABERTURA_PADRAO);
        form.setFechamento(c.getFechamento() != null ? c.getFechamento() : AgendamentoService.FECHAMENTO_PADRAO);
        m.addAttribute("comercioForm", form);
        preparar(m, c);
        return "cadastro";
    }

    @PostMapping("/editar")
    public String salvarEdicao(@PathVariable Long id, @Valid @ModelAttribute("comercioForm") ComercioForm form,
                               BindingResult br, Authentication auth, Model m, RedirectAttributes ra) {
        Comercio c = comercios.buscarDoDono(id, auth.getName());   // confere o dono ANTES de qualquer alteração
        if (!br.hasErrors()) {
            try {
                comercios.editar(id, form, auth.getName());
                ra.addFlashAttribute("ok", "Estabelecimento atualizado!");
                return "redirect:/meu-painel";
            } catch (ArquivoInvalidoException e) {
                br.rejectValue("fotoCapa", "foto.invalida", e.getMessage());   // erro junto ao campo da foto
            } catch (ExpedienteInvalidoException e) {
                br.rejectValue("fechamento", "expediente.invalido", e.getMessage());   // erro nos horários
            } catch (RegraNegocioException e) {
                br.rejectValue("categoriaId", "categoria.invalida", e.getMessage());   // ex.: troca bloqueada por agendamentos
            }
        }
        preparar(m, c);   // volta ao formulário com as mensagens de erro
        return "cadastro";
    }

    /**
     * ETAPA 3: exclui o comércio (e, em cascata, produtos, agendamentos e fotos).
     * Se o usuário logado não for o dono, o service lança AccessDeniedException -> HTTP 403.
     */
    @PostMapping("/excluir")
    public String excluir(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        comercios.excluirDoDono(id, auth.getName());
        ra.addFlashAttribute("ok", "Comércio excluído.");
        return "redirect:/meu-painel";
    }

    /** Dados extras que o template cadastro.html usa no modo edição. */
    private void preparar(Model m, Comercio c) {
        m.addAttribute("editando", true);
        m.addAttribute("acao", "/comercio/" + c.getId() + "/editar");
        m.addAttribute("categorias", comercios.listarCategorias());          // opções do campo "Tipo"
        m.addAttribute("categoriaOriginalId", c.getCategoria().getId());     // para avisar quando o tipo for trocado
        m.addAttribute("fotoAtual", c.getFotoCapa());
    }
}
