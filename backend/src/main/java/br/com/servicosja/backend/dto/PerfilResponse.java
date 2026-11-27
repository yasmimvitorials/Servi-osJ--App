package br.com.servicosja.backend.dto;

import br.com.servicosja.backend.model.enums.Perfil;

public record PerfilResponse(Long id, String nome, String email, String telefone,
    Perfil perfil, EnderecoResponse endereco) {
}
