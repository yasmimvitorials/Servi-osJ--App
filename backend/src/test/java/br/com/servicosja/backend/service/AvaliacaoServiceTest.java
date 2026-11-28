package br.com.servicosja.backend.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.servicosja.backend.dto.AvaliacaoRequest;
import br.com.servicosja.backend.dto.AvaliacaoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Avaliacao;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.AvaliacaoRepository;
import br.com.servicosja.backend.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)

public class AvaliacaoServiceTest {
	 private static final String EMAIL_CLIENTE = "maria@email.com";
	    private static final String EMAIL_PRESTADOR = "joao@email.com";

	    @Mock
	    private AvaliacaoRepository avaliacaoRepository;

	    @Mock
	    private PedidoRepository pedidoRepository;

	    @InjectMocks
	    private AvaliacaoService avaliacaoService;

	    private Prestador prestador;

	    private Pedido pedido(StatusPedido status) {
	        Cliente cliente = new Cliente();
	        cliente.setId(1L);
	        cliente.setNome("Maria");
	        cliente.setEmail(EMAIL_CLIENTE);

	        prestador = new Prestador();
	        prestador.setId(2L);
	        prestador.setNome("João");
	        prestador.setEmail(EMAIL_PRESTADOR);

	        Pedido p = new Pedido();
	        p.setId(100L);
	        p.setCliente(cliente);
	        p.setPrestador(prestador);
	        p.setStatus(status);
	        return p;
	    }

	    private void stubSalvar() {
	        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> {
	            Avaliacao a = inv.getArgument(0);
	            a.setId(50L);
	            return a;
	        });
	    }

	    @Test
	    @DisplayName("Cliente avalia o prestador e a nota média dele é atualizada")
	    void avaliar_clienteAvaliaPrestador_deveAtualizarNotaMedia() {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(StatusPedido.CONCLUIDO)));
	        when(avaliacaoRepository.existsByPedidoIdAndAvaliadorId(100L, 1L)).thenReturn(false);
	        stubSalvar();
	        when(avaliacaoRepository.calcularMediaPorAvaliado(2L)).thenReturn(Optional.of(4.5));

	        AvaliacaoResponse resposta = avaliacaoService.avaliar(EMAIL_CLIENTE, 100L,
	                new AvaliacaoRequest(5, "  Serviço excelente!  "));

	        ArgumentCaptor<Avaliacao> captor = ArgumentCaptor.forClass(Avaliacao.class);
	        verify(avaliacaoRepository).save(captor.capture());
	        assertEquals("Serviço excelente!", captor.getValue().getComentario());
	        assertEquals(1L, resposta.avaliadorId());
	        assertEquals(2L, resposta.avaliadoId());
	        assertEquals(5, resposta.nota());
	        assertEquals(4.5, prestador.getNotaMedia());
	    }

	    @Test
	    @DisplayName("A nota média é arredondada para duas casas")
	    void avaliar_deveArredondarNotaMedia() {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(StatusPedido.CONCLUIDO)));
	        when(avaliacaoRepository.existsByPedidoIdAndAvaliadorId(100L, 1L)).thenReturn(false);
	        stubSalvar();
	        when(avaliacaoRepository.calcularMediaPorAvaliado(2L)).thenReturn(Optional.of(4.3333333));

	        avaliacaoService.avaliar(EMAIL_CLIENTE, 100L, new AvaliacaoRequest(4, null));

	        assertEquals(4.33, prestador.getNotaMedia());
	    }

	    @Test
	    @DisplayName("Prestador avalia o cliente (sem recalcular nota de prestador)")
	    void avaliar_prestadorAvaliaCliente_naoDeveRecalcularMedia() {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(StatusPedido.CONCLUIDO)));
	        when(avaliacaoRepository.existsByPedidoIdAndAvaliadorId(100L, 2L)).thenReturn(false);
	        stubSalvar();

	        AvaliacaoResponse resposta = avaliacaoService.avaliar(EMAIL_PRESTADOR, 100L, new AvaliacaoRequest(4, "Cliente atencioso"));

	        assertEquals(2L, resposta.avaliadorId());
	        assertEquals(1L, resposta.avaliadoId());
	        verify(avaliacaoRepository, never()).calcularMediaPorAvaliado(any());
	    }

	    @Test
	    @DisplayName("Só pedidos concluídos podem ser avaliados")
	    void avaliar_pedidoNaoConcluido_deveLancarExcecao() {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(StatusPedido.EM_ANDAMENTO)));

	        assertThrows(RegraNegocioException.class,
	                () -> avaliacaoService.avaliar(EMAIL_CLIENTE, 100L, new AvaliacaoRequest(5, null)));
	        verify(avaliacaoRepository, never()).save(any());
	    }

	    @Test
	    @DisplayName("Não é possível avaliar duas vezes o mesmo pedido")
	    void avaliar_jaAvaliou_deveLancarExcecao() {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(StatusPedido.CONCLUIDO)));
	        when(avaliacaoRepository.existsByPedidoIdAndAvaliadorId(100L, 1L)).thenReturn(true);

	        assertThrows(RegraNegocioException.class,
	                () -> avaliacaoService.avaliar(EMAIL_CLIENTE, 100L, new AvaliacaoRequest(5, null)));
	        verify(avaliacaoRepository, never()).save(any());
	    }

	    @Test
	    @DisplayName("Quem não participa do pedido não pode avaliar (403)")
	    void avaliar_estranho_deveLancarAcessoNegado() {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(StatusPedido.CONCLUIDO)));

	        assertThrows(AcessoNegadoException.class,
	                () -> avaliacaoService.avaliar("intruso@email.com", 100L, new AvaliacaoRequest(1, "Falso")));
	        verify(avaliacaoRepository, never()).save(any());
	    }

	    @Test
	    @DisplayName("Pedido inexistente lança 'não encontrado'")
	    void avaliar_pedidoInexistente_deveLancarExcecao() {
	        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

	        assertThrows(RecursoNaoEncontradoException.class,
	                () -> avaliacaoService.avaliar(EMAIL_CLIENTE, 999L, new AvaliacaoRequest(5, null)));
	    }

	    @Test
	    @DisplayName("Lista as avaliações recebidas por um usuário")
	    void listarRecebidas_deveMapearAvaliacoes() {
	        Pedido pedido = pedido(StatusPedido.CONCLUIDO);
	        Avaliacao a = new Avaliacao();
	        a.setId(50L);
	        a.setPedido(pedido);
	        a.setAvaliador(pedido.getCliente());
	        a.setAvaliado(prestador);
	        a.setNota(5);
	        a.setComentario("Ótimo");
	        when(avaliacaoRepository.findByAvaliadoIdOrderByDataDesc(2L)).thenReturn(List.of(a));

	        List<AvaliacaoResponse> lista = avaliacaoService.listarRecebidas(2L);

	        assertEquals(1, lista.size());
	        assertEquals("Maria", lista.get(0).avaliadorNome());
	        assertEquals("Ótimo", lista.get(0).comentario());
	        assertEquals(100L, lista.get(0).pedidoId());
	    }
	}
