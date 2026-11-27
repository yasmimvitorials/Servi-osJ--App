package br.com.servicosja.backend.exception;
//E-mail ou senha incorretos//

public class CredenciaisInvalidasException  extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
