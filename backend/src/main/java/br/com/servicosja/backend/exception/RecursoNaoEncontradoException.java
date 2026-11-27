package br.com.servicosja.backend.exception;
//Recurso solicitado não existe//

public class RecursoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
