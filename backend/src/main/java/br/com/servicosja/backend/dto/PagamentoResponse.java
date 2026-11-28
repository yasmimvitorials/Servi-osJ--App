package br.com.servicosja.backend.dto;

import java.math.BigDecimal;

import br.com.servicosja.backend.model.enums.FormaPagamento;
import br.com.servicosja.backend.model.enums.StatusPagamento;

public record PagamentoResponse(Long id, Long pedidoId, BigDecimal valor, BigDecimal comissaoPlataforma,
        FormaPagamento forma, StatusPagamento status) {
}
