package br.com.conectabairro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** DTO da tela "Criar conta" (/registrar). A confirmação da senha é conferida no controller. */
@Getter @Setter
public class RegistroForm {

    @NotBlank(message = "Informe seu nome.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "E-mail inválido.")
    @Size(max = 120, message = "O e-mail deve ter no máximo 120 caracteres.")
    private String email;

    // BCrypt só considera os 72 primeiros bytes, por isso o limite máximo
    @NotBlank(message = "Crie uma senha.")
    @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres.")
    private String senha;

    @NotBlank(message = "Repita a senha.")
    private String confirmarSenha;
}
