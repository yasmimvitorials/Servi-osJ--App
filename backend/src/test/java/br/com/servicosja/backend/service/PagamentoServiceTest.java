package br.com.servicosja.backend.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.servicosja.backend.dto.PagamentoRequest;
import br.com.servicosja.backend.dto.PagamentoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.integration.PagamentoGateway;
import br.com.servicosja.backend.integration.ResultadoCobranca;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.Pagamento;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.enums.FormaPagamento;
import br.com.servicosja.backend.model.enums.StatusPagamento;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.PagamentoRepository;
import br.com.servicosja.backend.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
public class PagamentoServiceTest {
	private static final String EMAIL_CLIENTE = "maria@email.com";
    private static final String EMAIL_PRESTADOR = "joao@email.com";

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PagamentoGateway pagamentoGateway;

    private PagamentoService pagamentoService;

    @BeforeEach
    void setUp() {
        // comissão de 10%
        pagamentoService = new PagamentoService(pedidoRepository, pagamentoRepository, pagamentoGateway,
                new BigDecimal("10"));
    }

    private Pedido pedido(StatusPedido status, BigDecimal valorProposto, BigDecimal valorFinal) {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail(EMAIL_CLIENTE);
        Prestador prestador = new Prestador();
        prestador.setId(2L);
        prestador.setEmail(EMAIL_PRESTADOR);

        Pedido p = new Pedido();
        p.setId(100L);
        p.setCliente(cliente);
        p.setPrestador(prestador);
        p.setStatus(status);
        p.setValorProposto(valorProposto);
        p.setValorFinal(valorFinal);
        return p;
    }

    private void stubSalvar() {
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> {
            Pagamento p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
    }

    // ---------- pagar ----------

    @Test
    @DisplayName("Paga o valor final, calcula a comissão de 10% e marca como PAGO")
    void pagar_deveRegistrarPagamentoAprovado() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), new BigDecimal("180.00"))));
        when(pagamentoRepository.findByPedidoId(100L)).thenReturn(Optional.empty());
        when(pagamentoGateway.cobrar(any(Pagamento.class))).thenReturn(new ResultadoCobranca(true, "SIM-123"));
        stubSalvar();

        PagamentoResponse resposta = pagamentoService.pagar(EMAIL_CLIENTE, 100L, new PagamentoRequest(FormaPagamento.PIX));

        ArgumentCaptor<Pagamento> captor = ArgumentCaptor.forClass(Pagamento.class);
        verify(pagamentoRepository).save(captor.capture());
        assertEquals("SIM-123", captor.getValue().getIdTransacaoExterno());
        assertEquals(new BigDecimal("180.00"), resposta.valor());
        assertEquals(new BigDecimal("18.00"), resposta.comissaoPlataforma());
        assertEquals(FormaPagamento.PIX, resposta.forma());
        assertEquals(StatusPagamento.PAGO, resposta.status());
        assertEquals(100L, resposta.pedidoId());
    }

    @Test
    @DisplayName("Sem valor final, cobra o valor proposto")
    void pagar_semValorFinal_usaValorProposto() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), null)));
        when(pagamentoRepository.findByPedidoId(100L)).thenReturn(Optional.empty());
        when(pagamentoGateway.cobrar(any(Pagamento.class))).thenReturn(new ResultadoCobranca(true, "SIM-1"));
        stubSalvar();

        PagamentoResponse resposta = pagamentoService.pagar(EMAIL_CLIENTE, 100L, new PagamentoRequest(FormaPagamento.DINHEIRO));

        assertEquals(new BigDecimal("200.00"), resposta.valor());
        assertEquals(new BigDecimal("20.00"), resposta.comissaoPlataforma());
    }

    @Test
    @DisplayName("A comissão é arredondada para duas casas")
    void calcularComissao_deveArredondar() {
        assertEquals(new BigDecimal("3.33"), pagamentoService.calcularComissao(new BigDecimal("33.33")));
        assertEquals(new BigDecimal("0.01"), pagamentoService.calcularComissao(new BigDecimal("0.05")));
    }

    @Test
    @DisplayName("Só o cliente do pedido pode pagar (403)")
    void pagar_porOutroUsuario_deveLancarAcessoNegado() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), null)));

        assertThrows(AcessoNegadoException.class,
                () -> pagamentoService.pagar(EMAIL_PRESTADOR, 100L, new PagamentoRequest(FormaPagamento.PIX)));
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Só pedidos concluídos podem ser pagos")
    void pagar_pedidoNaoConcluido_deveLancarExcecao() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.EM_ANDAMENTO, new BigDecimal("200.00"), null)));

        assertThrows(RegraNegocioException.class,
                () -> pagamentoService.pagar(EMAIL_CLIENTE, 100L, new PagamentoRequest(FormaPagamento.PIX)));
        verify(pagamentoGateway, never()).cobrar(any());
    }

    @Test
    @DisplayName("Pedido já pago não pode ser pago de novo")
    void pagar_jaPago_deveLancarExcecao() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), null)));
        when(pagamentoRepository.findByPedidoId(100L)).thenReturn(Optional.of(new Pagamento()));

        assertThrows(RegraNegocioException.class,
                () -> pagamentoService.pagar(EMAIL_CLIENTE, 100L, new PagamentoRequest(FormaPagamento.PIX)));
        verify(pagamentoGateway, never()).cobrar(any());
    }

    @Test
    @DisplayName("Se o gateway recusar, nada é gravado")
    void pagar_gatewayRecusa_deveLancarExcecaoESemSalvar() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), null)));
        when(pagamentoRepository.findByPedidoId(100L)).thenReturn(Optional.empty());
        when(pagamentoGateway.cobrar(any(Pagamento.class))).thenReturn(new ResultadoCobranca(false, null));

        assertThrows(RegraNegocioException.class,
                () -> pagamentoService.pagar(EMAIL_CLIENTE, 100L, new PagamentoRequest(FormaPagamento.CARTAO_CREDITO)));
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pedido inexistente lança 'não encontrado'")
    void pagar_pedidoInexistente_deveLancarExcecao() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> pagamentoService.pagar(EMAIL_CLIENTE, 999L, new PagamentoRequest(FormaPagamento.PIX)));
    }

    // ---------- consultar ----------

    private Pagamento pagamentoExistente(Pedido pedido) {
        Pagamento p = new Pagamento();
        p.setId(1L);
        p.setPedido(pedido);
        p.setValor(new BigDecimal("180.00"));
        p.setComissaoPlataforma(new BigDecimal("18.00"));
        p.setForma(FormaPagamento.PIX);
        p.setStatus(StatusPagamento.PAGO);
        return p;
    }

    @Test
    @DisplayName("Cliente e prestador consultam o pagamento")
    void consultar_participantes_devemVer() {
        Pedido pedido = pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), new BigDecimal("180.00"));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.findByPedidoId(100L)).thenReturn(Optional.of(pagamentoExistente(pedido)));

        assertEquals(StatusPagamento.PAGO, pagamentoService.consultar(EMAIL_CLIENTE, 100L).status());
        assertEquals(new BigDecimal("18.00"), pagamentoService.consultar(EMAIL_PRESTADOR, 100L).comissaoPlataforma());
    }

    @Test
    @DisplayName("Estranho não consulta o pagamento (403)")
    void consultar_estranho_deveLancarAcessoNegado() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), null)));

        assertThrows(AcessoNegadoException.class, () -> pagamentoService.consultar("intruso@email.com", 100L));
    }

    @Test
    @DisplayName("Consultar pedido ainda não pago lança 'não encontrado'")
    void consultar_semPagamento_deveLancarExcecao() {
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(
                pedido(StatusPedido.CONCLUIDO, new BigDecimal("200.00"), null)));
        when(pagamentoRepository.findByPedidoId(100L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pagamentoService.consultar(EMAIL_CLIENTE, 100L));
    }
}
