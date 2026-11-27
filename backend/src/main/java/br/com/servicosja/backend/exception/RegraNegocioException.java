package br.com.servicosja.backend.exception;

// Violação de uma regra de negócio//
public class RegraNegocioException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public RegraNegocioException(String mensagem) {
		super(mensagem);
	}
}