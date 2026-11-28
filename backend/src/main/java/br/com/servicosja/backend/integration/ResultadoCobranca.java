package br.com.servicosja.backend.integration;

// Resposta do gateway de pagamento: aprovado ou não, e o identificador da transação no gateway. //

public record ResultadoCobranca(boolean aprovado, String idTransacao) {
}