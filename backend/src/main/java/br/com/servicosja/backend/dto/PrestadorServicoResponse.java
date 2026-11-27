package br.com.servicosja.backend.dto;

import java.math.BigDecimal;


public record PrestadorServicoResponse(Long servicoId, String servicoNome, String categoriaNome,
        BigDecimal precoBase) {
}