package br.com.conectabairro.controller;

import br.com.conectabairro.dto.RegistroForm;
import br.com.conectabairro.exception.RegraNegocioException;
import br.com.conectabairro.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Conta do usuário (lojista): criar conta e excluir a própria conta. */
@Controller @RequiredArgsConstructor
public class ContaController {
    private final UsuarioService usuarios;

    @GetMapping("/registrar")
    public String formRegistro(Model m) {
        m.addAttribute("registroForm", new RegistroForm());
        return "registrar";
    }

    @PostMapping("/registrar")
    public String registrar(@Valid @ModelAttribute("registroForm") RegistroForm form,
                            BindingResult br, RedirectAttributes ra) {
        // As duas senhas precisam ser iguais
        if (form.getSenha() != null && !form.getSenha().equals(form.getConfirmarSenha()))
            br.rejectValue("confirmarSenha", "senha.diferente", "As senhas não conferem.");

        if (!br.hasErrors()) {
            try {
                usuarios.registrar(form);
                ra.addFlashAttribute("ok", "Conta criada! Entre com seu e-mail e senha.");
                return "redirect:/login";
            } catch (RegraNegocioException e) {
                br.rejectValue("email", "email.duplicado", e.getMessage());   // e-mail já em uso
            }
        }
        return "registrar";   // volta ao formulário com as mensagens (a senha nunca é reexibida)
    }

    /**
     * ETAPA 2: exclui a conta do usuário logado (a confirmação com confirm() é feita no HTML).
     * Depois encerra a sessão (logout) e volta para a página inicial.
     */
    @PostMapping("/minha-conta/excluir")
    public String excluirConta(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
        usuarios.excluirConta(auth.getName());
        new SecurityContextLogoutHandler().logout(req, res, auth);   // invalida a sessão
        return "redirect:/?contaExcluida";
    }
}
