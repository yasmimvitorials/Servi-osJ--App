package br.com.servicosja.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.servicosja.backend.dto.MensagemRequest;
import br.com.servicosja.backend.dto.MensagemResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Mensagem;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Usuario;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.MensagemRepository;
import br.com.servicosja.backend.repository.PedidoRepository;

/**
 * Chat próprio, sem API externa: cada pedido tem uma conversa entre o cliente e o prestador que o aceitou.
 *
 * - só os dois participantes leem e escrevem;
 * - só existe depois que um prestador aceita (prestador convidado ainda não conversa);
 * - o histórico continua legível mesmo se o pedido for cancelado, mas só se escreve enquanto
 *   o pedido estiver ACEITO, EM_ANDAMENTO ou CONCLUIDO.
 */
@Service

public class ChatService {
	private final PedidoRepository pedidoRepository;
    private final MensagemRepository mensagemRepository;

    public ChatService(PedidoRepository pedidoRepository, MensagemRepository mensagemRepository) {
        this.pedidoRepository = pedidoRepository;
        this.mensagemRepository = mensagemRepository;
    }

    /** aposId é opcional: com ele, devolve só as mensagens mais novas que a última que o app já tem. */
    @Transactional(readOnly = true)
    public List<MensagemResponse> listar(String email, Long pedidoId, Long aposId) {
        Pedido pedido = buscarPedido(pedidoId);
        identificarAutor(pedido, email);

        long a = (aposId == null) ? 0L : aposId;
        return mensagemRepository.findByPedidoIdAndIdGreaterThanOrderByIdAsc(pedidoId, a).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MensagemResponse enviar(String email, Long pedidoId, MensagemRequest request) {
        Pedido pedido = buscarPedido(pedidoId);
        Usuario autor = identificarAutor(pedido, email);

        StatusPedido status = pedido.getStatus();
        if (status != StatusPedido.ACEITO && status != StatusPedido.EM_ANDAMENTO && status != StatusPedido.CONCLUIDO) {
            throw new RegraNegocioException("O chat está encerrado para este pedido.");
        }

        Mensagem mensagem = new Mensagem();
        mensagem.setPedido(pedido);
        mensagem.setAutor(autor);
        mensagem.setTexto(request.texto().trim());
        return toResponse(mensagemRepository.save(mensagem));
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    /** Confere se quem chama é o cliente ou o prestador do pedido e devolve esse usuário. */
    private Usuario identificarAutor(Pedido pedido, String email) {
        boolean ehCliente = pedido.getCliente().getEmail().equals(email);
        boolean ehPrestador = pedido.getPrestador() != null && pedido.getPrestador().getEmail().equals(email);

        if (!ehCliente && !ehPrestador) {
            throw new AcessoNegadoException("Somente o cliente e o prestador do pedido participam do chat.");
        }
        if (pedido.getPrestador() == null) {
            throw new RegraNegocioException("O chat só fica disponível depois que um prestador aceita o pedido.");
        }
        return ehCliente ? pedido.getCliente() : pedido.getPrestador();
    }

    private MensagemResponse toResponse(Mensagem m) {
        return new MensagemResponse(m.getId(), m.getAutor().getId(), m.getAutor().getNome(),
                m.getTexto(), m.getEnviadaEm());
    }
}
