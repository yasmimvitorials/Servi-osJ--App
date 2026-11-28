package br.com.servicosja.backend.dto;

import java.time.LocalDateTime;

public record MensagemResponse(Long id, Long autorId, String autorNome, String texto, LocalDateTime enviadaEm) {
}