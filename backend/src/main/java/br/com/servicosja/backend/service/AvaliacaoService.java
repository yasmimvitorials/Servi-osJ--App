package br.com.servicosja.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.servicosja.backend.dto.AvaliacaoRequest;
import br.com.servicosja.backend.dto.AvaliacaoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Avaliacao;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.Usuario;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.AvaliacaoRepository;
import br.com.servicosja.backend.repository.PedidoRepository;

/**
 * Avaliações entre cliente e prestador.
 * - só pedidos CONCLUIDOS podem ser avaliados para evitar avaliação falsa
 * - cada participante avalia a outra parte uma única vez por pedido
 * - quando o avaliado é o prestador, a nota média dele é recalculada
 */
@Service

public class AvaliacaoService {
	private final AvaliacaoRepository avaliacaoRepository;
    private final PedidoRepository pedidoRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, PedidoRepository pedidoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public AvaliacaoResponse avaliar(String email, Long pedidoId, AvaliacaoRequest request) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.CONCLUIDO) {
            throw new RegraNegocioException("Só é possível avaliar pedidos concluídos.");
        }

        Prestador prestador = pedido.getPrestador();
        Usuario avaliador;
        Usuario avaliado;
        if (pedido.getCliente().getEmail().equals(email)) {
            avaliador = pedido.getCliente();
            avaliado = prestador;
        } else if (prestador != null && prestador.getEmail().equals(email)) {
            avaliador = prestador;
            avaliado = pedido.getCliente();
        } else {
            throw new AcessoNegadoException("Somente o cliente e o prestador do pedido podem avaliar.");
        }

        if (avaliacaoRepository.existsByPedidoIdAndAvaliadorId(pedidoId, avaliador.getId())) {
            throw new RegraNegocioException("Você já avaliou este pedido.");
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setPedido(pedido);
        avaliacao.setAvaliador(avaliador);
        avaliacao.setAvaliado(avaliado);
        avaliacao.setNota(request.nota());
        avaliacao.setComentario(request.comentario() == null ? null : request.comentario().trim());
        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        if (avaliado instanceof Prestador p) {
            atualizarNotaMedia(p);
        }
        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarRecebidas(Long usuarioId) {
        return avaliacaoRepository.findByAvaliadoIdOrderByDataDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void atualizarNotaMedia(Prestador prestador) {
        double media = avaliacaoRepository.calcularMediaPorAvaliado(prestador.getId()).orElse(0.0);
        prestador.setNotaMedia(Math.round(media * 100.0) / 100.0);
    }

    private AvaliacaoResponse toResponse(Avaliacao a) {
        return new AvaliacaoResponse(a.getId(), a.getPedido().getId(), a.getAvaliador().getId(),
                a.getAvaliador().getNome(), a.getAvaliado().getId(), a.getNota(), a.getComentario(), a.getData());
    }
}
