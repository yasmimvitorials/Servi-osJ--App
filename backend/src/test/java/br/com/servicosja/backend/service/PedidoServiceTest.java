package br.com.servicosja.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.servicosja.backend.dto.ConviteResponse;
import br.com.servicosja.backend.dto.EnderecoRequest;
import br.com.servicosja.backend.dto.PedidoRequest;
import br.com.servicosja.backend.dto.PedidoResponse;
import br.com.servicosja.backend.dto.ResultadoViabilidade;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Administrador;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.ConvitePedido;
import br.com.servicosja.backend.model.Endereco;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.Servico;
import br.com.servicosja.backend.model.enums.Modalidade;
import br.com.servicosja.backend.model.enums.StatusConvite;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.ClienteRepository;
import br.com.servicosja.backend.repository.ConvitePedidoRepository;
import br.com.servicosja.backend.repository.PedidoRepository;
import br.com.servicosja.backend.repository.ServicoRepository;
import br.com.servicosja.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)

@ExtendWith(MockitoExtension.class)

public class PedidoServiceTest {
	private static final String EMAIL_CLIENTE = "maria@email.com";
    private static final String EMAIL_PRESTADOR = "joao@email.com";

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private ConvitePedidoRepository convitePedidoRepository;

    @Mock
    private MatchingService matchingService;

    @Mock
    private ViabilidadeService viabilidadeService;

    @InjectMocks
    private PedidoService pedidoService;

    // ---------- helpers ----------

    private Cliente cliente(Long id, String email) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome("Maria");
        c.setEmail(email);
        return c;
    }

    private Prestador prestador(Long id, String email) {
        Prestador p = new Prestador();
        p.setId(id);
        p.setNome("João");
        p.setEmail(email);
        return p;
    }

    private Servico servico() {
        Servico s = new Servico();
        s.setId(10L);
        s.setNome("Pintura de parede");
        return s;
    }

    private EnderecoRequest enderecoRequest() {
        return new EnderecoRequest("Rua do Serviço", "20", "57240-000", "São Miguel dos Campos", -9.78, -36.09);
    }

    private PedidoRequest request(Modalidade modalidade, boolean urgente, LocalDateTime data, EnderecoRequest endereco) {
        return new PedidoRequest(10L, "  Pintar a sala  ", new BigDecimal("100.00"), modalidade, urgente, data,
                endereco, List.of("http://foto/1.jpg"));
    }

    private void stubCriacao(Cliente cliente) {
        when(clienteRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente));
        when(servicoRepository.findById(10L)).thenReturn(Optional.of(servico()));
    }

    private Pedido pedido(Cliente cliente, Prestador prestador, StatusPedido status) {
        Pedido p = new Pedido();
        p.setId(100L);
        p.setCliente(cliente);
        p.setPrestador(prestador);
        p.setServico(servico());
        p.setStatus(status);
        p.setEndereco(new Endereco());
        return p;
    }

    // ---------- criarPedido ----------

    @Test
    @DisplayName("Cria pedido ABERTO com o endereço informado e as fotos")
    void criarPedido_comEnderecoInformado_deveSalvarPedidoAberto() {
        Cliente cliente = cliente(1L, EMAIL_CLIENTE);
        stubCriacao(cliente);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PedidoResponse resposta = pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.HOJE, true, null, enderecoRequest()));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido salvo = captor.getValue();

        assertEquals(StatusPedido.ABERTO, salvo.getStatus());
        assertEquals("Pintar a sala", salvo.getDescricao());
        assertEquals("Rua do Serviço", salvo.getEndereco().getRua());
        assertEquals("57240000", salvo.getEndereco().getCep());
        assertEquals(1, salvo.getFotos().size());
        assertEquals(100L, resposta.id());
        assertEquals(StatusPedido.ABERTO, resposta.status());
        assertEquals("Pintura de parede", resposta.servicoNome());
        assertNull(resposta.prestadorId());
    }

    @Test
    @DisplayName("Sem endereço no pedido, usa uma cópia do endereço do perfil")
    void criarPedido_semEndereco_usaCopiaDoEnderecoDoPerfil() {
        Cliente cliente = cliente(1L, EMAIL_CLIENTE);
        Endereco doPerfil = new Endereco();
        doPerfil.setRua("Rua do Perfil");
        doPerfil.setCidade("Maceió");
        doPerfil.setLatitude(-9.66);
        doPerfil.setLongitude(-35.73);
        cliente.setEndereco(doPerfil);
        stubCriacao(cliente);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.criarPedido(EMAIL_CLIENTE, request(Modalidade.ESTA_SEMANA, false, null, null));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Endereco usado = captor.getValue().getEndereco();

        assertNotSame(doPerfil, usado);
        assertEquals("Rua do Perfil", usado.getRua());
        assertEquals(-9.66, usado.getLatitude());
    }

    @Test
    @DisplayName("Sem endereço no pedido nem no perfil, recusa")
    void criarPedido_semNenhumEndereco_deveLancarExcecao() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));

        assertThrows(RegraNegocioException.class,
                () -> pedidoService.criarPedido(EMAIL_CLIENTE, request(Modalidade.HOJE, false, null, null)));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agendado sem data é recusado")
    void criarPedido_agendadoSemData_deveLancarExcecao() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));

        assertThrows(RegraNegocioException.class, () -> pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.AGENDADO, false, null, enderecoRequest())));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agendado com data no passado é recusado")
    void criarPedido_agendadoNoPassado_deveLancarExcecao() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));

        assertThrows(RegraNegocioException.class, () -> pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.AGENDADO, false, LocalDateTime.now().minusDays(1), enderecoRequest())));
    }

    @Test
    @DisplayName("Agendado com data futura é aceito e mantém a data")
    void criarPedido_agendadoNoFuturo_deveManterData() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime data = LocalDateTime.now().plusDays(3);

        PedidoResponse resposta = pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.AGENDADO, false, data, enderecoRequest()));

        assertEquals(data, resposta.dataHoraServico());
    }

    @Test
    @DisplayName("Pedido urgente só pode ser para hoje")
    void criarPedido_urgenteAgendado_deveLancarExcecao() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));

        assertThrows(RegraNegocioException.class, () -> pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.AGENDADO, true, LocalDateTime.now().plusDays(1), enderecoRequest())));
    }

    @Test
    @DisplayName("Fora de AGENDADO, a data informada é ignorada")
    void criarPedido_hojeComData_deveIgnorarData() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponse resposta = pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.HOJE, false, LocalDateTime.now().plusDays(2), enderecoRequest()));

        assertNull(resposta.dataHoraServico());
    }

    @Test
    @DisplayName("Serviço inexistente lança 'não encontrado'")
    void criarPedido_servicoInexistente_deveLancarExcecao() {
        when(clienteRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente(1L, EMAIL_CLIENTE)));
        when(servicoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.criarPedido(EMAIL_CLIENTE,
                request(Modalidade.HOJE, false, null, enderecoRequest())));
    }

    @Test
    @DisplayName("Usuário que não é cliente lança 'não encontrado'")
    void criarPedido_clienteInexistente_deveLancarExcecao() {
        when(clienteRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.criarPedido("joao@email.com",
                request(Modalidade.HOJE, false, null, enderecoRequest())));
    }

    // ---------- listarMeus ----------

    @Test
    @DisplayName("Cliente lista os pedidos que criou")
    void listarMeus_cliente_deveListarPedidosDoCliente() {
        Cliente cliente = cliente(1L, EMAIL_CLIENTE);
        when(usuarioRepository.findByEmail(EMAIL_CLIENTE)).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByClienteIdOrderByCriadoEmDesc(1L))
                .thenReturn(List.of(pedido(cliente, null, StatusPedido.ABERTO)));

        List<PedidoResponse> lista = pedidoService.listarMeus(EMAIL_CLIENTE);

        assertEquals(1, lista.size());
        assertEquals(StatusPedido.ABERTO, lista.get(0).status());
    }

    @Test
    @DisplayName("Prestador lista os pedidos que aceitou")
    void listarMeus_prestador_deveListarPedidosDoPrestador() {
        Prestador prestador = prestador(2L, EMAIL_PRESTADOR);
        when(usuarioRepository.findByEmail(EMAIL_PRESTADOR)).thenReturn(Optional.of(prestador));
        when(pedidoRepository.findByPrestadorIdOrderByCriadoEmDesc(2L))
                .thenReturn(List.of(pedido(cliente(1L, EMAIL_CLIENTE), prestador, StatusPedido.ACEITO)));

        List<PedidoResponse> lista = pedidoService.listarMeus(EMAIL_PRESTADOR);

        assertEquals(1, lista.size());
        assertEquals(2L, lista.get(0).prestadorId());
    }

    @Test
    @DisplayName("Administrador não possui pedidos")
    void listarMeus_admin_deveLancarExcecao() {
        when(usuarioRepository.findByEmail("admin@servicoja.com")).thenReturn(Optional.of(new Administrador()));

        assertThrows(RegraNegocioException.class, () -> pedidoService.listarMeus("admin@servicoja.com"));
    }

    // ---------- obterPedido ----------

    @Test
    @DisplayName("O cliente dono vê o pedido")
    void obterPedido_donoCliente_deveRetornar() {
        Cliente cliente = cliente(1L, EMAIL_CLIENTE);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido(cliente, null, StatusPedido.ABERTO)));

        PedidoResponse resposta = pedidoService.obterPedido(EMAIL_CLIENTE, 100L);

        assertEquals(100L, resposta.id());
    }

    @Test
    @DisplayName("O prestador que aceitou vê o pedido")
    void obterPedido_prestadorAtribuido_deveRetornar() {
        Prestador prestador = prestador(2L, EMAIL_PRESTADOR);
        when(pedidoRepository.findById(100L))
                .thenReturn(Optional.of(pedido(cliente(1L, EMAIL_CLIENTE), prestador, StatusPedido.ACEITO)));

        assertEquals(100L, pedidoService.obterPedido(EMAIL_PRESTADOR, 100L).id());
    }

    @Test
    @DisplayName("Um estranho não vê o pedido (403)")
    void obterPedido_estranho_deveLancarAcessoNegado() {
        when(pedidoRepository.findById(100L))
                .thenReturn(Optional.of(pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO)));
        when(usuarioRepository.findByEmail("outro@email.com")).thenReturn(Optional.of(cliente(9L, "outro@email.com")));

        assertThrows(AcessoNegadoException.class, () -> pedidoService.obterPedido("outro@email.com", 100L));
    }

    @Test
    @DisplayName("Administrador vê qualquer pedido")
    void obterPedido_admin_deveRetornar() {
        when(pedidoRepository.findById(100L))
                .thenReturn(Optional.of(pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO)));
        when(usuarioRepository.findByEmail("admin@servicoja.com")).thenReturn(Optional.of(new Administrador()));

        assertEquals(100L, pedidoService.obterPedido("admin@servicoja.com", 100L).id());
    }

    @Test
    @DisplayName("Pedido inexistente lança 'não encontrado'")
    void obterPedido_inexistente_deveLancarExcecao() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.obterPedido(EMAIL_CLIENTE, 999L));
    }

    // ---------- cancelarPedido ----------

    @Test
    @DisplayName("Cliente cancela pedido aberto")
    void cancelar_pedidoAberto_deveCancelar() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.cancelarPedido(EMAIL_CLIENTE, 100L);

        assertEquals(StatusPedido.CANCELADO, resposta.status());
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    @DisplayName("Cliente cancela pedido já aceito")
    void cancelar_pedidoAceito_deveCancelar() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.ACEITO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertEquals(StatusPedido.CANCELADO, pedidoService.cancelarPedido(EMAIL_CLIENTE, 100L).status());
    }

    @Test
    @DisplayName("Pedido concluído não pode ser cancelado")
    void cancelar_pedidoConcluido_deveLancarExcecao() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.CONCLUIDO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> pedidoService.cancelarPedido(EMAIL_CLIENTE, 100L));
        assertEquals(StatusPedido.CONCLUIDO, pedido.getStatus());
    }

    @Test
    @DisplayName("Só o cliente dono pode cancelar (o prestador não)")
    void cancelar_porOutroUsuario_deveLancarAcessoNegado() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.ACEITO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(AcessoNegadoException.class, () -> pedidoService.cancelarPedido(EMAIL_PRESTADOR, 100L));
        assertEquals(StatusPedido.ACEITO, pedido.getStatus());
    }

    // =====================================================================
    // Parte 2: convites, aceite, recusa, início e conclusão
    // =====================================================================

    private ConvitePedido convite(Pedido pedido, Prestador prestador, StatusConvite status) {
        ConvitePedido c = new ConvitePedido();
        c.setPedido(pedido);
        c.setPrestador(prestador);
        c.setStatus(status);
        pedido.getConvites().add(c);
        return c;
    }

    @Test
    @DisplayName("Ao criar o pedido, convida os prestadores escolhidos pelo MatchingService")
    void criarPedido_deveEnviarConvites() {
        stubCriacao(cliente(1L, EMAIL_CLIENTE));
        Prestador a = prestador(2L, "a@email.com");
        Prestador b = prestador(3L, "b@email.com");
        when(matchingService.selecionarPrestadores(any(Pedido.class))).thenReturn(List.of(a, b));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        pedidoService.criarPedido(EMAIL_CLIENTE, request(Modalidade.HOJE, false, null, enderecoRequest()));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        List<ConvitePedido> convites = captor.getValue().getConvites();

        assertEquals(2, convites.size());
        assertEquals(StatusConvite.ENVIADO, convites.get(0).getStatus());
        assertEquals(a, convites.get(0).getPrestador());
        assertEquals(b, convites.get(1).getPrestador());
    }

    @Test
    @DisplayName("Lista convites pendentes com os dados do pedido e a viabilidade")
    void listarConvites_deveMontarRespostaComViabilidade() {
        Prestador joao = prestador(2L, EMAIL_PRESTADOR);
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        pedido.setValorProposto(new BigDecimal("100.00"));
        pedido.setModalidade(Modalidade.HOJE);
        ConvitePedido convite = convite(pedido, joao, StatusConvite.ENVIADO);

        ResultadoViabilidade viabilidade = new ResultadoViabilidade(true, 5.0, 10,
                new BigDecimal("15.00"), new BigDecimal("85.00"), null);
        when(convitePedidoRepository.findByPrestadorEmailAndStatusAndPedidoStatusOrderByEnviadoEmDesc(
                EMAIL_PRESTADOR, StatusConvite.ENVIADO, StatusPedido.ABERTO)).thenReturn(List.of(convite));
        when(viabilidadeService.calcular(pedido, joao)).thenReturn(viabilidade);

        List<ConviteResponse> lista = pedidoService.listarConvites(EMAIL_PRESTADOR);

        assertEquals(1, lista.size());
        assertEquals(100L, lista.get(0).pedidoId());
        assertEquals("Pintura de parede", lista.get(0).servicoNome());
        assertEquals(new BigDecimal("100.00"), lista.get(0).valorProposto());
        assertTrue(lista.get(0).viabilidade().viavel());
    }

    @Test
    @DisplayName("Prestador convidado consegue ver o pedido antes de aceitar")
    void obterPedido_prestadorConvidado_deveRetornar() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        convite(pedido, prestador(2L, EMAIL_PRESTADOR), StatusConvite.ENVIADO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertEquals(100L, pedidoService.obterPedido(EMAIL_PRESTADOR, 100L).id());
    }

    @Test
    @DisplayName("Aceitar: o prestador assume o pedido e os outros convites expiram")
    void aceitar_deveAtribuirPrestadorEExpirarOutrosConvites() {
        Prestador a = prestador(2L, "a@email.com");
        Prestador b = prestador(3L, "b@email.com");
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        ConvitePedido conviteA = convite(pedido, a, StatusConvite.ENVIADO);
        ConvitePedido conviteB = convite(pedido, b, StatusConvite.ENVIADO);
        when(pedidoRepository.buscarParaAtualizar(100L)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.aceitarPedido("a@email.com", 100L);

        assertEquals(StatusPedido.ACEITO, resposta.status());
        assertEquals(2L, resposta.prestadorId());
        assertEquals(StatusConvite.ACEITO, conviteA.getStatus());
        assertNotNull(conviteA.getRespondidoEm());
        assertEquals(StatusConvite.EXPIRADO, conviteB.getStatus());
    }

    @Test
    @DisplayName("Quem não foi convidado não pode aceitar (403)")
    void aceitar_semConvite_deveLancarAcessoNegado() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        convite(pedido, prestador(2L, "a@email.com"), StatusConvite.ENVIADO);
        when(pedidoRepository.buscarParaAtualizar(100L)).thenReturn(Optional.of(pedido));

        assertThrows(AcessoNegadoException.class, () -> pedidoService.aceitarPedido("intruso@email.com", 100L));
        assertEquals(StatusPedido.ABERTO, pedido.getStatus());
    }

    @Test
    @DisplayName("Pedido já aceito por outro prestador não pode mais ser aceito")
    void aceitar_pedidoJaAceito_deveLancarExcecao() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, "a@email.com"), StatusPedido.ACEITO);
        convite(pedido, prestador(2L, "a@email.com"), StatusConvite.ACEITO);
        convite(pedido, prestador(3L, "b@email.com"), StatusConvite.EXPIRADO);
        when(pedidoRepository.buscarParaAtualizar(100L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> pedidoService.aceitarPedido("b@email.com", 100L));
    }

    @Test
    @DisplayName("Convite já recusado não pode ser aceito depois")
    void aceitar_conviteRecusado_deveLancarExcecao() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        convite(pedido, prestador(2L, "a@email.com"), StatusConvite.RECUSADO);
        when(pedidoRepository.buscarParaAtualizar(100L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> pedidoService.aceitarPedido("a@email.com", 100L));
    }

    @Test
    @DisplayName("Aceitar pedido inexistente lança 'não encontrado'")
    void aceitar_pedidoInexistente_deveLancarExcecao() {
        when(pedidoRepository.buscarParaAtualizar(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> pedidoService.aceitarPedido("a@email.com", 999L));
    }

    @Test
    @DisplayName("Recusar marca o convite como RECUSADO e mantém o pedido aberto")
    void recusar_deveMarcarConviteRecusado() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        ConvitePedido convite = convite(pedido, prestador(2L, "a@email.com"), StatusConvite.ENVIADO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        pedidoService.recusarConvite("a@email.com", 100L);

        assertEquals(StatusConvite.RECUSADO, convite.getStatus());
        assertNotNull(convite.getRespondidoEm());
        assertEquals(StatusPedido.ABERTO, pedido.getStatus());
    }

    @Test
    @DisplayName("Não é possível recusar um convite já respondido")
    void recusar_conviteJaRespondido_deveLancarExcecao() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        convite(pedido, prestador(2L, "a@email.com"), StatusConvite.RECUSADO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> pedidoService.recusarConvite("a@email.com", 100L));
    }

    @Test
    @DisplayName("Quem não foi convidado não pode recusar (403)")
    void recusar_semConvite_deveLancarAcessoNegado() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(AcessoNegadoException.class, () -> pedidoService.recusarConvite("intruso@email.com", 100L));
    }

    @Test
    @DisplayName("O prestador do pedido inicia o serviço")
    void iniciar_deveMudarParaEmAndamento() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.ACEITO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertEquals(StatusPedido.EM_ANDAMENTO, pedidoService.iniciarPedido(EMAIL_PRESTADOR, 100L).status());
    }

    @Test
    @DisplayName("Outro prestador não pode iniciar o pedido")
    void iniciar_porOutroPrestador_deveLancarAcessoNegado() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.ACEITO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(AcessoNegadoException.class, () -> pedidoService.iniciarPedido("outro@email.com", 100L));
    }

    @Test
    @DisplayName("Só se inicia um pedido que está ACEITO")
    void iniciar_foraDeAceito_deveLancarExcecao() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.EM_ANDAMENTO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> pedidoService.iniciarPedido(EMAIL_PRESTADOR, 100L));
    }

    @Test
    @DisplayName("Concluir com valor final informado")
    void concluir_comValorFinal_deveGuardarValor() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.EM_ANDAMENTO);
        pedido.setValorProposto(new BigDecimal("100.00"));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.concluirPedido(EMAIL_PRESTADOR, 100L, new BigDecimal("130.00"));

        assertEquals(StatusPedido.CONCLUIDO, resposta.status());
        assertEquals(new BigDecimal("130.00"), resposta.valorFinal());
    }

    @Test
    @DisplayName("Concluir sem valor final usa o valor proposto")
    void concluir_semValorFinal_usaValorProposto() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.EM_ANDAMENTO);
        pedido.setValorProposto(new BigDecimal("100.00"));
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.concluirPedido(EMAIL_PRESTADOR, 100L, null);

        assertEquals(new BigDecimal("100.00"), resposta.valorFinal());
    }

    @Test
    @DisplayName("Só se conclui um pedido EM_ANDAMENTO")
    void concluir_foraDeAndamento_deveLancarExcecao() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.ACEITO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraNegocioException.class, () -> pedidoService.concluirPedido(EMAIL_PRESTADOR, 100L, null));
    }

    @Test
    @DisplayName("O cliente não pode concluir o pedido (403)")
    void concluir_porOutroUsuario_deveLancarAcessoNegado() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), prestador(2L, EMAIL_PRESTADOR), StatusPedido.EM_ANDAMENTO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        assertThrows(AcessoNegadoException.class, () -> pedidoService.concluirPedido(EMAIL_CLIENTE, 100L, null));
    }

    @Test
    @DisplayName("Cancelar o pedido expira os convites ainda pendentes")
    void cancelar_deveExpirarConvitesPendentes() {
        Pedido pedido = pedido(cliente(1L, EMAIL_CLIENTE), null, StatusPedido.ABERTO);
        ConvitePedido pendente = convite(pedido, prestador(2L, "a@email.com"), StatusConvite.ENVIADO);
        ConvitePedido recusado = convite(pedido, prestador(3L, "b@email.com"), StatusConvite.RECUSADO);
        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));

        pedidoService.cancelarPedido(EMAIL_CLIENTE, 100L);

        assertEquals(StatusConvite.EXPIRADO, pendente.getStatus());
        assertEquals(StatusConvite.RECUSADO, recusado.getStatus());
    }
}
