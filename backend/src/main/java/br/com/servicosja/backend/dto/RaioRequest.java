package br.com.servicosja.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RaioRequest(
	@NotNull(message = "O raio é obrigatório")
    @DecimalMin(value = "1.0", message = "O raio mínimo é 1 km")
    @DecimalMax(value = "200.0", message = "O raio máximo é 200 km") Double raioAtuacaoKm) {
}


