package br.com.conectabairro.exception;

/** Erro de regra de negócio (ex.: horário ocupado). A mensagem é mostrada ao usuário. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
