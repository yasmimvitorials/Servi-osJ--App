package br.com.servicosja.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensagemRequest(
	 @NotBlank(message = "A mensagem não pode estar vazia")
     @Size(max = 1000, message = "A mensagem deve ter no máximo 1000 caracteres") String texto) {
}
