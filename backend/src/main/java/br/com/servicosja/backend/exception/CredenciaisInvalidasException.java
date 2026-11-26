package br.com.servicosja.backend.exception;
//E-mail ou senha incorretos//

public class CredenciaisInvalidasException  extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
