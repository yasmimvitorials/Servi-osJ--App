package br.com.servicosja.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PrestadorServicoRequest(@NotNull(message = "O serviço é obrigatório") Long servicoId,
		@DecimalMin(value = "0.0", message = "O preço não pode ser negativo") BigDecimal precoBase) {
}
