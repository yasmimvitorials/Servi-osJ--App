package br.com.servicosja.backend.integration;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.servicosja.backend.model.Pagamento;

// Gateway de faz de conta: aprova toda cobrança. Nenhum dinheiro real é movimentado. //
@Component
public class PagamentoGatewaySimulado implements PagamentoGateway {
	@Override
    public ResultadoCobranca cobrar(Pagamento pagamento) {
        return new ResultadoCobranca(true, "SIM-" + UUID.randomUUID());
    }
}
