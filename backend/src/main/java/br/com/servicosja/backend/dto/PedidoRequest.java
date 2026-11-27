package br.com.servicosja.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.servicosja.backend.model.enums.Modalidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar um pedido.
 * - endereco: local do serviço. Se omitido, usa o endereço do perfil do cliente.
 * - dataHoraServico: obrigatória somente quando modalidade = AGENDADO.
 * - fotos: URLs das fotos (o envio do arquivo em si é responsabilidade do app).
 */

public record PedidoRequest(
	 @NotNull(message = "O serviço é obrigatório") Long servicoId,
     @NotBlank(message = "A descrição é obrigatória")
     @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres") String descricao,
     @NotNull(message = "O valor proposto é obrigatório")
     @DecimalMin(value = "0.01", message = "O valor proposto deve ser maior que zero") BigDecimal valorProposto,
     @NotNull(message = "A modalidade é obrigatória (HOJE, ESTA_SEMANA ou AGENDADO)") Modalidade modalidade,
     boolean urgente,
     LocalDateTime dataHoraServico,
     @Valid EnderecoRequest endereco,
     @Size(max = 5, message = "No máximo 5 fotos") List<String> fotos) {
}
