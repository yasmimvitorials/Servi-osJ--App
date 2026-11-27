package br.com.servicosja.backend.dto;

public record EnderecoResponse(String rua, String numero, String cep, String cidade,
Double latitude, Double longitude) {
}