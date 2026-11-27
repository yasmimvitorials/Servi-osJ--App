package br.com.servicosja.backend.dto;

import java.time.LocalTime;

import br.com.servicosja.backend.model.enums.DiaSemana;

public record DisponibilidadeResponse(DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFim) {
}