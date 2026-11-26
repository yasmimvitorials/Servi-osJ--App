package br.com.servicosja.backend.exception;
// Violação de uma regra de negócio//
public class RegraNegocioException extends RuntimeException {
	public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}