package br.com.servicosja.backend.integration;

import br.com.servicosja.backend.model.Pagamento;

public interface PagamentoGateway {

    ResultadoCobranca cobrar(Pagamento pagamento);
}
