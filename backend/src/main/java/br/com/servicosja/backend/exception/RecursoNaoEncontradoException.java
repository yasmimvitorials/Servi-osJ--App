package br.com.servicosja.backend.exception;
//Recurso solicitado não existe//

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
