package br.com.servicosja.backend.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.servicosja.backend.dto.ResultadoViabilidade;
import br.com.servicosja.backend.model.Endereco;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.PrestadorServico;
import br.com.servicosja.backend.model.Servico;

public class ViabilidadeServiceTest {

    // custo de R$ 1,50 por km e velocidade média de 30 km/h
    private final ViabilidadeService service = new ViabilidadeService(new BigDecimal("1.50"), 30.0);

    private Endereco endereco(Double lat, Double lon) {
        Endereco e = new Endereco();
        e.setLatitude(lat);
        e.setLongitude(lon);
        return e;
    }

    private Servico servico() {
        Servico s = new Servico();
        s.setId(10L);
        return s;
    }

    /** Prestador em (-9.78, -36.09). Se precoBase != null, ele oferece o serviço 10 por esse preço. */
    private Prestador prestador(Endereco endereco, double raioKm, BigDecimal precoBase) {
        Prestador p = new Prestador();
        p.setEndereco(endereco);
        p.setRaioAtuacaoKm(raioKm);
        if (precoBase != null) {
            PrestadorServico ps = new PrestadorServico();
            ps.setServico(servico());
            ps.setPrecoBase(precoBase);
            p.getServicos().add(ps);
        }
        return p;
    }

    private Pedido pedido(Endereco endereco, BigDecimal valor) {
        Pedido p = new Pedido();
        p.setServico(servico());
        p.setEndereco(endereco);
        p.setValorProposto(valor);
        return p;
    }

    @Test
    @DisplayName("Pedido próximo e com valor bom é viável")
    void calcular_pedidoCompensa_deveSerViavel() {
        Prestador prestador = prestador(endereco(-9.78, -36.09), 10.0, new BigDecimal("50.00"));
        Pedido pedido = pedido(endereco(-9.735, -36.09), new BigDecimal("100.00"));   // ~5 km

        ResultadoViabilidade r = service.calcular(pedido, prestador);

        assertTrue(r.viavel());
        assertNull(r.motivo());
        assertEquals(5.0, r.distanciaKm(), 0.1);
        assertEquals(10, r.tempoEstimadoMin());
        assertEquals(new BigDecimal("15.00"), r.custoDeslocamento());
        assertEquals(new BigDecimal("85.00"), r.valorLiquido());
    }

    @Test
    @DisplayName("Pedido fora do raio de atuação não é viável")
    void calcular_foraDoRaio_naoDeveSerViavel() {
        Prestador prestador = prestador(endereco(-9.78, -36.09), 10.0, null);
        Pedido pedido = pedido(endereco(-9.48, -36.09), new BigDecimal("500.00"));   // ~33 km

        ResultadoViabilidade r = service.calcular(pedido, prestador);

        assertFalse(r.viavel());
        assertNotNull(r.motivo());
        assertTrue(r.distanciaKm() > 10.0);
    }

    @Test
    @DisplayName("Valor líquido abaixo do preço mínimo do prestador não é viável")
    void calcular_valorBaixo_naoDeveSerViavel() {
        Prestador prestador = prestador(endereco(-9.78, -36.09), 10.0, new BigDecimal("50.00"));
        Pedido pedido = pedido(endereco(-9.735, -36.09), new BigDecimal("40.00"));   // líquido 25,00 < 50,00

        ResultadoViabilidade r = service.calcular(pedido, prestador);

        assertFalse(r.viavel());
        assertEquals(new BigDecimal("25.00"), r.valorLiquido());
        assertNotNull(r.motivo());
    }

    @Test
    @DisplayName("Sem preço base definido para o serviço, só o raio e o lucro positivo importam")
    void calcular_semPrecoBase_usaZeroComoMinimo() {
        Prestador prestador = prestador(endereco(-9.78, -36.09), 10.0, null);
        Pedido pedido = pedido(endereco(-9.735, -36.09), new BigDecimal("30.00"));

        assertTrue(service.calcular(pedido, prestador).viavel());
    }

    @Test
    @DisplayName("Pedido sem coordenadas não é viável")
    void calcular_semCoordenadasNoPedido_naoDeveSerViavel() {
        Prestador prestador = prestador(endereco(-9.78, -36.09), 10.0, null);
        Pedido pedido = pedido(endereco(null, null), new BigDecimal("100.00"));

        ResultadoViabilidade r = service.calcular(pedido, prestador);

        assertFalse(r.viavel());
        assertNotNull(r.motivo());
    }

    @Test
    @DisplayName("Prestador sem endereço cadastrado não é viável")
    void calcular_prestadorSemEndereco_naoDeveSerViavel() {
        Prestador prestador = prestador(null, 10.0, null);
        Pedido pedido = pedido(endereco(-9.735, -36.09), new BigDecimal("100.00"));

        assertFalse(service.calcular(pedido, prestador).viavel());
    }
}
