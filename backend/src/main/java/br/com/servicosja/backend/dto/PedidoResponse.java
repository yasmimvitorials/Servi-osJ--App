package br.com.servicosja.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.servicosja.backend.model.enums.Modalidade;
import br.com.servicosja.backend.model.enums.StatusPedido;

public record PedidoResponse(
        Long id,
        StatusPedido status,
        Long servicoId,
        String servicoNome,
        Long clienteId,
        String clienteNome,
        Long prestadorId,
        String prestadorNome,
        String descricao,
        BigDecimal valorProposto,
        BigDecimal valorFinal,
        Modalidade modalidade,
        boolean urgente,
        LocalDateTime dataHoraServico,
        LocalDateTime criadoEm,
        EnderecoResponse endereco,
        List<String> fotos) {
}
