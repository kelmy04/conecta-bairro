package br.com.conectabairro.exception;

/** Horário de abertura/fechamento inválido (ex.: fechamento antes da abertura). Mostrado junto ao campo. */
public class ExpedienteInvalidoException extends RegraNegocioException {
    public ExpedienteInvalidoException(String mensagem) { super(mensagem); }
}
