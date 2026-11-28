package br.com.servicosja.backend.dto;

import java.time.LocalDateTime;

public record AvaliacaoResponse(Long id, Long pedidoId, Long avaliadorId, String avaliadorNome,
        Long avaliadoId, int nota, String comentario, LocalDateTime data) {
}
