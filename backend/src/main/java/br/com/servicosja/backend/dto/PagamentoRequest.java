package br.com.servicosja.backend.dto;

import br.com.servicosja.backend.model.enums.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(
	@NotNull(message = "A forma de pagamento é obrigatória") FormaPagamento forma) {
	}
