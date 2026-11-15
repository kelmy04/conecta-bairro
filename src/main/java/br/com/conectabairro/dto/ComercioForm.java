package br.com.conectabairro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalTime;

/**
 * CORREÇÃO B3: DTO (Data Transfer Object) do formulário de cadastro de comércio.
 *
 * Antes, o formulário preenchia a entidade Comercio diretamente: campos vazios causavam
 * NullPointerException e o usuário podia enviar campos extras (ex.: "id"), o que permitia
 * sobrescrever outro comércio. Agora o formulário só aceita os campos abaixo, todos validados.
 */
@Getter @Setter
public class ComercioForm {

    @NotBlank(message = "Informe o nome do comércio.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotNull(message = "Escolha o tipo do comércio.")
    private Long categoriaId;

    @NotBlank(message = "Informe o endereço.")
    @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres.")
    private String endereco;

    // Aceita máscara, ex.: (65) 99999-0000. Exige de 10 a 15 dígitos no total.
    @NotBlank(message = "Informe o WhatsApp com DDD.")
    @Size(max = 20, message = "O WhatsApp deve ter no máximo 20 caracteres.")
    @Pattern(regexp = "\\D*(\\d\\D*){10,15}", message = "WhatsApp inválido: informe DDD e número (10 a 15 dígitos).")
    private String whatsapp;

    // ETAPA 1: a foto agora é um ARQUIVO enviado pelo formulário (JPG/PNG até 3 MB, opcional).
    // O caminho salvo no banco (coluna foto_capa) é gerado pelo ArmazenamentoService.
    private MultipartFile fotoCapa;

    // Expediente da agenda: obrigatório só para barbearia (conferido no ComercioService, que sabe a categoria).
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime abertura;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime fechamento;
}
