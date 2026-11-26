package br.com.servicosja.backend.dto;

import br.com.servicosja.backend.model.enums.Perfil;

public record AuthResponse(String token, String tipo, Long id, String nome, Perfil perfil) {

    public AuthResponse(String token, Long id, String nome, Perfil perfil) {
        this(token, "Bearer", id, nome, perfil);
    }
}