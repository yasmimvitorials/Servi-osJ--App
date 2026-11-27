package br.com.servicosja.backend.dto;

import java.time.LocalTime;

import br.com.servicosja.backend.model.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;

public record DisponibilidadeRequest(
		@NotNull(message = "O dia da semana é obrigatório") DiaSemana diaSemana,
		@NotNull(message = "A hora de início é obrigatória") LocalTime horaInicio,
		@NotNull(message = "A hora de fim é obrigatória") LocalTime horaFim) {
}
