package br.com.conectabairro.exception;

import br.com.conectabairro.controller.ComercioController;
import br.com.conectabairro.controller.ContaController;
import br.com.conectabairro.controller.DashboardController;
import br.com.conectabairro.controller.PainelController;
import br.com.conectabairro.controller.VitrineController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * CORREÇÃO B2: tratamento de erros das telas (controllers MVC).
 *
 * Antes, abrir /comercio/999 (inexistente) gerava a página de erro 500. Agora a
 * RegraNegocioException é capturada aqui e mostramos a página amigável "erro.html"
 * com o status HTTP correto (404). Obs.: usamos "return da view + setStatus" e não um
 * redirect, pois um redirect responderia 302 e perderia o status 404.
 * O ApiExceptionHandler continua cuidando apenas da API REST (JSON).
 */
@ControllerAdvice(assignableTypes = {VitrineController.class, PainelController.class,
                                   ComercioController.class, DashboardController.class,
                                   ContaController.class})
public class WebExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public String naoEncontrado(RegraNegocioException e, Model model, HttpServletResponse resposta) {
        resposta.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("mensagem", e.getMessage());
        return "erro";   // templates/erro.html
    }
}
