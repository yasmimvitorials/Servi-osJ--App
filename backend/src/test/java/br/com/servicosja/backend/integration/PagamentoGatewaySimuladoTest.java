package br.com.servicosja.backend.integration;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.servicosja.backend.model.Pagamento;
public class PagamentoGatewaySimuladoTest {
	private final PagamentoGatewaySimulado gateway = new PagamentoGatewaySimulado();

    @Test
    @DisplayName("O gateway simulado aprova a cobrança e devolve um identificador")
    void cobrar_deveAprovar() {
        ResultadoCobranca resultado = gateway.cobrar(new Pagamento());

        assertTrue(resultado.aprovado());
        assertTrue(resultado.idTransacao().startsWith("SIM-"));
    }

    @Test
    @DisplayName("Cada cobrança recebe um identificador diferente")
    void cobrar_deveGerarIdsDiferentes() {
        assertNotEquals(gateway.cobrar(new Pagamento()).idTransacao(), gateway.cobrar(new Pagamento()).idTransacao());
    }
}
