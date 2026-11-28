package br.com.servicosja.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;

public record ConcluirPedidoRequest(
	@DecimalMin(value = "0.01", message = "O valor final deve ser maior que zero") BigDecimal valorFinal) {
	}