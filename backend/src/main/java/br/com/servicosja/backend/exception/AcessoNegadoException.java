package br.com.servicosja.backend.exception;

//O usuário está logado, mas não tem permissão para este recurso (resulta em HTTP 403).//

public class AcessoNegadoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
