package br.com.servicosja.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.servicosja.backend.dto.PagamentoRequest;
import br.com.servicosja.backend.dto.PagamentoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.integration.PagamentoGateway;
import br.com.servicosja.backend.integration.ResultadoCobranca;
import br.com.servicosja.backend.model.Pagamento;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.enums.StatusPagamento;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.PagamentoRepository;
import br.com.servicosja.backend.repository.PedidoRepository;

//Pagamento do pedido concluído. A cobrança passa pelo PagamentoGateway (hoje simulado) e a plataforma/
@Service

public class PagamentoService {
	private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoGateway pagamentoGateway;
    private final BigDecimal comissaoPercentual;

    public PagamentoService(PedidoRepository pedidoRepository,
                            PagamentoRepository pagamentoRepository,
                            PagamentoGateway pagamentoGateway,
                            @Value("${pagamento.comissao-percentual:10}") BigDecimal comissaoPercentual) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.pagamentoGateway = pagamentoGateway;
        this.comissaoPercentual = comissaoPercentual;
    }

    @Transactional
    public PagamentoResponse pagar(String emailCliente, Long pedidoId, PagamentoRequest request) {
        Pedido pedido = buscarPedido(pedidoId);

        if (!pedido.getCliente().getEmail().equals(emailCliente)) {
            throw new AcessoNegadoException("Somente o cliente do pedido pode pagá-lo.");
        }
        if (pedido.getStatus() != StatusPedido.CONCLUIDO) {
            throw new RegraNegocioException("Só é possível pagar pedidos concluídos.");
        }
        if (pagamentoRepository.findByPedidoId(pedidoId).isPresent()) {
            throw new RegraNegocioException("Este pedido já foi pago.");
        }

        BigDecimal valor = pedido.getValorFinal() != null ? pedido.getValorFinal() : pedido.getValorProposto();

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setValor(valor);
        pagamento.setComissaoPlataforma(calcularComissao(valor));
        pagamento.setForma(request.forma());
        pagamento.setStatus(StatusPagamento.PENDENTE);

        ResultadoCobranca cobranca = pagamentoGateway.cobrar(pagamento);
        if (!cobranca.aprovado()) {
            throw new RegraNegocioException("Pagamento recusado. Tente novamente.");
        }
        pagamento.setStatus(StatusPagamento.PAGO);
        pagamento.setIdTransacaoExterno(cobranca.idTransacao());

        return toResponse(pagamentoRepository.save(pagamento));
    }

    @Transactional(readOnly = true)
    public PagamentoResponse consultar(String email, Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);

        boolean participante = pedido.getCliente().getEmail().equals(email)
                || (pedido.getPrestador() != null && pedido.getPrestador().getEmail().equals(email));
        if (!participante) {
            throw new AcessoNegadoException("Você não tem acesso ao pagamento deste pedido.");
        }
        return pagamentoRepository.findByPedidoId(pedidoId)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este pedido ainda não foi pago."));
    }

    BigDecimal calcularComissao(BigDecimal valor) {
        return valor.multiply(comissaoPercentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    private PagamentoResponse toResponse(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getValor(),
                p.getComissaoPlataforma(), p.getForma(), p.getStatus());
    }
}
