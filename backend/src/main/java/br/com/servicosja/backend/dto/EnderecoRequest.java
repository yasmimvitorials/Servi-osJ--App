package br.com.servicosja.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

//latitude e longitude que vem do app (GPS do celular feita no Android). //

public record EnderecoRequest(
	 @NotBlank(message = "A rua é obrigatória") String rua,
     String numero,
     @Pattern(regexp = "^$|\\d{5}-?\\d{3}", message = "CEP inválido") String cep,
     @NotBlank(message = "A cidade é obrigatória") String cidade,
     @NotNull(message = "A latitude é obrigatória")
     @DecimalMin(value = "-90.0", message = "Latitude inválida")
     @DecimalMax(value = "90.0", message = "Latitude inválida") Double latitude,
     @NotNull(message = "A longitude é obrigatória")
     @DecimalMin(value = "-180.0", message = "Longitude inválida")
     @DecimalMax(value = "180.0", message = "Longitude inválida") Double longitude) {
}
