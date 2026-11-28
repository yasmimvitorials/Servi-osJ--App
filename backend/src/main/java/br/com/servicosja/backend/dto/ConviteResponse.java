package br.com.servicosja.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.servicosja.backend.model.enums.Modalidade;

public record ConviteResponse(Long pedidoId,
        String servicoNome,
        String descricao,
        BigDecimal valorProposto,
        Modalidade modalidade,
        boolean urgente,
        LocalDateTime dataHoraServico,
        EnderecoResponse endereco,
        LocalDateTime enviadoEm,
        ResultadoViabilidade viabilidade) {
}
