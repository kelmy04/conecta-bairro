package br.com.conectabairro.exception;

/** Erro específico de upload (arquivo grande demais, formato inválido...). Mostrado junto ao campo da foto. */
public class ArquivoInvalidoException extends RegraNegocioException {
    public ArquivoInvalidoException(String mensagem) { super(mensagem); }
}
