package br.com.servicosja.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

//Substitui TODA a agenda semanal do prestador. Lista vazia = sem horários.//

public record DefinirDisponibilidadeRequest(@NotNull(message = "A lista de horários é obrigatória") @Valid List<DisponibilidadeRequest> horarios) {
}
