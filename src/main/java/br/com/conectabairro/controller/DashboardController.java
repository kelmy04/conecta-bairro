package br.com.conectabairro.controller;

import br.com.conectabairro.service.ComercioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** ETAPA 2: tela inicial do lojista depois do login ("Meus comércios"). */
@Controller @RequiredArgsConstructor
public class DashboardController {
    private final ComercioService comercios;

    @GetMapping("/meu-painel")
    public String meusComercios(Authentication auth, Model model) {
        // auth.getName() = e-mail do usuário logado (definido no UsuarioDetailsService)
        model.addAttribute("comercios", comercios.listarDoDono(auth.getName()));
        return "meus-comercios";
    }
}
