package br.com.servicosja.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarPerfilRequest(
	@NotBlank(message = "O nome é obrigatório") String nome,
    @NotBlank(message = "O telefone é obrigatório") String telefone) {
}
