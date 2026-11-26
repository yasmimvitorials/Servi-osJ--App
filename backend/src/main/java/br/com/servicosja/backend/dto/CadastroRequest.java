package br.com.servicosja.backend.dto;

import org.hibernate.validator.constraints.br.CPF;

import br.com.servicosja.backend.model.enums.Perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
	 @NotBlank(message = "O nome é obrigatório") String nome,
     @NotBlank(message = "O CPF é obrigatório") @CPF(message = "CPF inválido") String cpf,
     @NotBlank(message = "O e-mail é obrigatório") @Email(message = "E-mail inválido") String email,
     @NotBlank(message = "O telefone é obrigatório") String telefone,
     @NotBlank(message = "A senha é obrigatória") @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres") String senha,
     @NotNull(message = "O perfil é obrigatório (CLIENTE ou PRESTADOR)") Perfil perfil) {
}