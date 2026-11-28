package br.com.servicosja.backend.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import br.com.servicosja.backend.dto.MensagemRequest;
import br.com.servicosja.backend.dto.MensagemResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.Mensagem;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.MensagemRepository;
import br.com.servicosja.backend.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)

public class ChatServiceTest {
	 private static final String EMAIL_CLIENTE = "maria@email.com";
	    private static final String EMAIL_PRESTADOR = "joao@email.com";

	    @Mock
	    private PedidoRepository pedidoRepository;

	    @Mock
	    private MensagemRepository mensagemRepository;

	    @InjectMocks
	    private ChatService chatService;

	    private Cliente cliente() {
	        Cliente c = new Cliente();
	        c.setId(1L);
	        c.setNome("Maria");
	        c.setEmail(EMAIL_CLIENTE);
	        return c;
	    }

	    private Prestador prestador() {
	        Prestador p = new Prestador();
	        p.setId(2L);
	        p.setNome("João");
	        p.setEmail(EMAIL_PRESTADOR);
	        return p;
	    }

	    private Pedido pedido(boolean comPrestador, StatusPedido status) {
	        Pedido p = new Pedido();
	        p.setId(100L);
	        p.setCliente(cliente());
	        p.setPrestador(comPrestador ? prestador() : null);
	        p.setStatus(status);
	        return p;
	    }

	    private void stubPedido(Pedido pedido) {
	        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
	    }

	    private Mensagem mensagem(Long id, Pedido pedido, String texto) {
	        Mensagem m = new Mensagem();
	        m.setId(id);
	        m.setPedido(pedido);
	        m.setAutor(pedido.getCliente());
	        m.setTexto(texto);
	        return m;
	    }

	    // ---------- enviar ----------

	    @Test
	    @DisplayName("O cliente envia mensagem e aparece como autor")
	    void enviar_comoCliente_deveSalvar() {
	        stubPedido(pedido(true, StatusPedido.ACEITO));
	        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> {
	            Mensagem m = inv.getArgument(0);
	            m.setId(7L);
	            return m;
	        });

	        MensagemResponse resposta = chatService.enviar(EMAIL_CLIENTE, 100L, new MensagemRequest("  Posso ir às 14h?  "));

	        ArgumentCaptor<Mensagem> captor = ArgumentCaptor.forClass(Mensagem.class);
	        verify(mensagemRepository).save(captor.capture());
	        assertEquals("Posso ir às 14h?", captor.getValue().getTexto());
	        assertEquals(1L, resposta.autorId());
	        assertEquals("Maria", resposta.autorNome());
	        assertEquals(7L, resposta.id());
	    }

	    @Test
	    @DisplayName("O prestador envia mensagem com o pedido em andamento")
	    void enviar_comoPrestador_deveSalvar() {
	        stubPedido(pedido(true, StatusPedido.EM_ANDAMENTO));
	        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

	        MensagemResponse resposta = chatService.enviar(EMAIL_PRESTADOR, 100L, new MensagemRequest("Cheguei"));

	        assertEquals(2L, resposta.autorId());
	        assertEquals("João", resposta.autorNome());
	    }

	    @Test
	    @DisplayName("Ainda é possível conversar depois de o pedido ser concluído")
	    void enviar_pedidoConcluido_devePermitir() {
	        stubPedido(pedido(true, StatusPedido.CONCLUIDO));
	        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

	        chatService.enviar(EMAIL_CLIENTE, 100L, new MensagemRequest("Obrigada!"));

	        verify(mensagemRepository).save(any(Mensagem.class));
	    }

	    @Test
	    @DisplayName("Não se envia mensagem em pedido cancelado")
	    void enviar_pedidoCancelado_deveLancarExcecao() {
	        stubPedido(pedido(true, StatusPedido.CANCELADO));

	        assertThrows(RegraNegocioException.class,
	                () -> chatService.enviar(EMAIL_CLIENTE, 100L, new MensagemRequest("Oi")));
	        verify(mensagemRepository, never()).save(any());
	    }

	    @Test
	    @DisplayName("Sem prestador (pedido aberto) o chat ainda não existe")
	    void enviar_semPrestador_deveLancarExcecao() {
	        stubPedido(pedido(false, StatusPedido.ABERTO));

	        assertThrows(RegraNegocioException.class,
	                () -> chatService.enviar(EMAIL_CLIENTE, 100L, new MensagemRequest("Oi")));
	        verify(mensagemRepository, never()).save(any());
	    }

	    @Test
	    @DisplayName("Quem não participa do pedido não pode escrever (403)")
	    void enviar_estranho_deveLancarAcessoNegado() {
	        stubPedido(pedido(true, StatusPedido.ACEITO));

	        assertThrows(AcessoNegadoException.class,
	                () -> chatService.enviar("intruso@email.com", 100L, new MensagemRequest("Oi")));
	        verify(mensagemRepository, never()).save(any());
	    }

	    @Test
	    @DisplayName("Pedido inexistente lança 'não encontrado'")
	    void enviar_pedidoInexistente_deveLancarExcecao() {
	        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

	        assertThrows(RecursoNaoEncontradoException.class,
	                () -> chatService.enviar(EMAIL_CLIENTE, 999L, new MensagemRequest("Oi")));
	    }

	    // ---------- listar ----------

	    @Test
	    @DisplayName("Sem aposId, lista desde o início (id maior que 0)")
	    void listar_semAposId_deveListarTudo() {
	        Pedido pedido = pedido(true, StatusPedido.ACEITO);
	        stubPedido(pedido);
	        when(mensagemRepository.findByPedidoIdAndIdGreaterThanOrderByIdAsc(100L, 0L))
	                .thenReturn(List.of(mensagem(1L, pedido, "Oi"), mensagem(2L, pedido, "Tudo bem?")));

	        List<MensagemResponse> lista = chatService.listar(EMAIL_PRESTADOR, 100L, null);

	        assertEquals(2, lista.size());
	        assertEquals("Oi", lista.get(0).texto());
	        assertEquals("Maria", lista.get(0).autorNome());
	    }

	    @Test
	    @DisplayName("Com aposId, consulta só as mensagens mais novas")
	    void listar_comAposId_deveFiltrar() {
	        Pedido pedido = pedido(true, StatusPedido.EM_ANDAMENTO);
	        stubPedido(pedido);
	        when(mensagemRepository.findByPedidoIdAndIdGreaterThanOrderByIdAsc(100L, 5L))
	                .thenReturn(List.of(mensagem(6L, pedido, "Nova")));

	        List<MensagemResponse> lista = chatService.listar(EMAIL_CLIENTE, 100L, 5L);

	        assertEquals(1, lista.size());
	        assertEquals(6L, lista.get(0).id());
	    }

	    @Test
	    @DisplayName("O histórico de um pedido cancelado continua legível")
	    void listar_pedidoCancelado_devePermitirLeitura() {
	        stubPedido(pedido(true, StatusPedido.CANCELADO));
	        when(mensagemRepository.findByPedidoIdAndIdGreaterThanOrderByIdAsc(100L, 0L)).thenReturn(List.of());

	        assertTrue(chatService.listar(EMAIL_CLIENTE, 100L, null).isEmpty());
	    }

	    @Test
	    @DisplayName("Quem não participa do pedido não pode ler (403)")
	    void listar_estranho_deveLancarAcessoNegado() {
	        stubPedido(pedido(true, StatusPedido.ACEITO));

	        assertThrows(AcessoNegadoException.class, () -> chatService.listar("intruso@email.com", 100L, null));
	    }

	    @Test
	    @DisplayName("Sem prestador, listar também é recusado")
	    void listar_semPrestador_deveLancarExcecao() {
	        stubPedido(pedido(false, StatusPedido.ABERTO));

	        assertThrows(RegraNegocioException.class, () -> chatService.listar(EMAIL_CLIENTE, 100L, null));
	    }
	}
