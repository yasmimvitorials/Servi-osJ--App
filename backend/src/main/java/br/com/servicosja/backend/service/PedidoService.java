package br.com.servicosja.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.servicosja.backend.dto.ConviteResponse;
import br.com.servicosja.backend.dto.EnderecoRequest;
import br.com.servicosja.backend.dto.EnderecoResponse;
import br.com.servicosja.backend.dto.PedidoRequest;
import br.com.servicosja.backend.dto.PedidoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.ConvitePedido;
import br.com.servicosja.backend.model.Endereco;
import br.com.servicosja.backend.model.FotoPedido;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.Servico;
import br.com.servicosja.backend.model.Usuario;
import br.com.servicosja.backend.model.enums.Modalidade;
import br.com.servicosja.backend.model.enums.Perfil;
import br.com.servicosja.backend.model.enums.StatusConvite;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.repository.ClienteRepository;
import br.com.servicosja.backend.repository.ConvitePedidoRepository;
import br.com.servicosja.backend.repository.PedidoRepository;
import br.com.servicosja.backend.repository.ServicoRepository;
import br.com.servicosja.backend.repository.UsuarioRepository;

@Service


public class PedidoService {
	 private final PedidoRepository pedidoRepository;
	    private final ClienteRepository clienteRepository;
	    private final UsuarioRepository usuarioRepository;
	    private final ServicoRepository servicoRepository;
	    private final ConvitePedidoRepository convitePedidoRepository;
	    private final MatchingService matchingService;
	    private final ViabilidadeService viabilidadeService;

	    public PedidoService(PedidoRepository pedidoRepository,
	                         ClienteRepository clienteRepository,
	                         UsuarioRepository usuarioRepository,
	                         ServicoRepository servicoRepository,
	                         ConvitePedidoRepository convitePedidoRepository,
	                         MatchingService matchingService,
	                         ViabilidadeService viabilidadeService) {
	        this.pedidoRepository = pedidoRepository;
	        this.clienteRepository = clienteRepository;
	        this.usuarioRepository = usuarioRepository;
	        this.servicoRepository = servicoRepository;
	        this.convitePedidoRepository = convitePedidoRepository;
	        this.matchingService = matchingService;
	        this.viabilidadeService = viabilidadeService;
	    }

	    // ================= CLIENTE =================

	    @Transactional
	    public PedidoResponse criarPedido(String emailCliente, PedidoRequest request) {
	        Cliente cliente = clienteRepository.findByEmail(emailCliente)
	                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
	        Servico servico = servicoRepository.findById(request.servicoId())
	                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));

	        validarModalidade(request);

	        Pedido pedido = new Pedido();
	        pedido.setCliente(cliente);
	        pedido.setServico(servico);
	        pedido.setDescricao(request.descricao().trim());
	        pedido.setValorProposto(request.valorProposto());
	        pedido.setModalidade(request.modalidade());
	        pedido.setUrgente(request.urgente());
	        pedido.setDataHoraServico(request.modalidade() == Modalidade.AGENDADO ? request.dataHoraServico() : null);
	        pedido.setStatus(StatusPedido.ABERTO);
	        pedido.setEndereco(definirEndereco(cliente, request.endereco()));

	        if (request.fotos() != null) {
	            for (String url : request.fotos()) {
	                FotoPedido foto = new FotoPedido();
	                foto.setPedido(pedido);
	                foto.setUrl(url);
	                pedido.getFotos().add(foto);
	            }
	        }

	        // prioridade inteligente: convida os prestadores selecionados (gravados junto com o pedido)
	        for (Prestador prestador : matchingService.selecionarPrestadores(pedido)) {
	            ConvitePedido convite = new ConvitePedido();
	            convite.setPedido(pedido);
	            convite.setPrestador(prestador);
	            pedido.getConvites().add(convite);
	        }

	        return toResponse(pedidoRepository.save(pedido));
	    }

	    @Transactional
	    public PedidoResponse cancelarPedido(String emailCliente, Long pedidoId) {
	        Pedido pedido = buscarPedido(pedidoId);

	        if (!pedido.getCliente().getEmail().equals(emailCliente)) {
	            throw new AcessoNegadoException("Somente o cliente que criou o pedido pode cancelá-lo.");
	        }
	        if (pedido.getStatus() != StatusPedido.ABERTO && pedido.getStatus() != StatusPedido.ACEITO) {
	            throw new RegraNegocioException("Só é possível cancelar pedidos abertos ou aceitos.");
	        }

	        pedido.setStatus(StatusPedido.CANCELADO);
	        expirarConvitesPendentes(pedido);
	        return toResponse(pedido);
	    }

	    // ================= CONSULTAS (cliente e prestador) =================

	    /** Cliente: pedidos que criou. Prestador: pedidos que aceitou. */
	    @Transactional(readOnly = true)
	    public List<PedidoResponse> listarMeus(String email) {
	        Usuario usuario = usuarioRepository.findByEmail(email)
	                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

	        List<Pedido> pedidos;
	        if (usuario.getPerfil() == Perfil.CLIENTE) {
	            pedidos = pedidoRepository.findByClienteIdOrderByCriadoEmDesc(usuario.getId());
	        } else if (usuario.getPerfil() == Perfil.PRESTADOR) {
	            pedidos = pedidoRepository.findByPrestadorIdOrderByCriadoEmDesc(usuario.getId());
	        } else {
	            throw new RegraNegocioException("Este perfil não possui pedidos.");
	        }
	        return pedidos.stream().map(this::toResponse).toList();
	    }

	    @Transactional(readOnly = true)
	    public PedidoResponse obterPedido(String email, Long pedidoId) {
	        Pedido pedido = buscarPedido(pedidoId);
	        exigirAcesso(pedido, email);
	        return toResponse(pedido);
	    }

	    // ================= PRESTADOR =================

	    /** Pedidos abertos para os quais o prestador foi convidado e ainda não respondeu. */
	    @Transactional(readOnly = true)
	    public List<ConviteResponse> listarConvites(String emailPrestador) {
	        return convitePedidoRepository
	                .findByPrestadorEmailAndStatusAndPedidoStatusOrderByEnviadoEmDesc(
	                        emailPrestador, StatusConvite.ENVIADO, StatusPedido.ABERTO)
	                .stream()
	                .map(this::toConviteResponse)
	                .toList();
	    }

	    /** O primeiro prestador a aceitar leva o pedido; os demais convites expiram. */
	    @Transactional
	    public PedidoResponse aceitarPedido(String emailPrestador, Long pedidoId) {
	        // travamos a linha para impedir dois aceites simultâneos
	        Pedido pedido = pedidoRepository.buscarParaAtualizar(pedidoId)
	                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
	        ConvitePedido convite = buscarConvite(pedido, emailPrestador);

	        if (pedido.getStatus() != StatusPedido.ABERTO || convite.getStatus() != StatusConvite.ENVIADO) {
	            throw new RegraNegocioException("Este pedido não está mais disponível para você.");
	        }

	        pedido.setPrestador(convite.getPrestador());
	        pedido.setStatus(StatusPedido.ACEITO);
	        convite.setStatus(StatusConvite.ACEITO);
	        convite.setRespondidoEm(LocalDateTime.now());
	        expirarConvitesPendentes(pedido);

	        return toResponse(pedido);
	    }

	    @Transactional
	    public void recusarConvite(String emailPrestador, Long pedidoId) {
	        Pedido pedido = buscarPedido(pedidoId);
	        ConvitePedido convite = buscarConvite(pedido, emailPrestador);

	        if (convite.getStatus() != StatusConvite.ENVIADO) {
	            throw new RegraNegocioException("Este convite já foi respondido ou expirou.");
	        }
	        convite.setStatus(StatusConvite.RECUSADO);
	        convite.setRespondidoEm(LocalDateTime.now());
	    }

	    @Transactional
	    public PedidoResponse iniciarPedido(String emailPrestador, Long pedidoId) {
	        Pedido pedido = buscarPedido(pedidoId);
	        exigirPrestadorDoPedido(pedido, emailPrestador);

	        if (pedido.getStatus() != StatusPedido.ACEITO) {
	            throw new RegraNegocioException("Só é possível iniciar um pedido que foi aceito.");
	        }
	        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
	        return toResponse(pedido);
	    }

	    /** valorFinal é opcional: se não vier, vale o valor proposto. */
	    @Transactional
	    public PedidoResponse concluirPedido(String emailPrestador, Long pedidoId, BigDecimal valorFinal) {
	        Pedido pedido = buscarPedido(pedidoId);
	        exigirPrestadorDoPedido(pedido, emailPrestador);

	        if (pedido.getStatus() != StatusPedido.EM_ANDAMENTO) {
	            throw new RegraNegocioException("Só é possível concluir um pedido que está em andamento.");
	        }
	        pedido.setValorFinal(valorFinal != null ? valorFinal : pedido.getValorProposto());
	        pedido.setStatus(StatusPedido.CONCLUIDO);
	        return toResponse(pedido);
	    }

	    // ================= regras auxiliares =================

	    private void validarModalidade(PedidoRequest request) {
	        if (request.urgente() && request.modalidade() != Modalidade.HOJE) {
	            throw new RegraNegocioException("Pedidos urgentes devem ser para hoje.");
	        }
	        if (request.modalidade() == Modalidade.AGENDADO) {
	            if (request.dataHoraServico() == null) {
	                throw new RegraNegocioException("Informe a data e a hora do serviço agendado.");
	            }
	            if (!request.dataHoraServico().isAfter(LocalDateTime.now())) {
	                throw new RegraNegocioException("A data do serviço agendado deve estar no futuro.");
	            }
	        }
	    }

	    /** O pedido guarda uma CÓPIA do endereço: se o cliente mudar de casa depois, o pedido antigo não muda. */
	    private Endereco definirEndereco(Cliente cliente, EnderecoRequest informado) {
	        Endereco novo = new Endereco();
	        if (informado != null) {
	            novo.setRua(informado.rua().trim());
	            novo.setNumero(informado.numero());
	            novo.setCep(informado.cep() == null ? null : informado.cep().replaceAll("\\D", ""));
	            novo.setCidade(informado.cidade().trim());
	            novo.setLatitude(informado.latitude());
	            novo.setLongitude(informado.longitude());
	            return novo;
	        }

	        Endereco perfil = cliente.getEndereco();
	        if (perfil == null) {
	            throw new RegraNegocioException(
	                    "Informe o endereço do serviço ou cadastre um endereço no seu perfil.");
	        }
	        novo.setRua(perfil.getRua());
	        novo.setNumero(perfil.getNumero());
	        novo.setCep(perfil.getCep());
	        novo.setCidade(perfil.getCidade());
	        novo.setLatitude(perfil.getLatitude());
	        novo.setLongitude(perfil.getLongitude());
	        return novo;
	    }

	    private Pedido buscarPedido(Long id) {
	        return pedidoRepository.findById(id)
	                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
	    }

	    private ConvitePedido buscarConvite(Pedido pedido, String emailPrestador) {
	        return pedido.getConvites().stream()
	                .filter(c -> c.getPrestador().getEmail().equals(emailPrestador))
	                .findFirst()
	                .orElseThrow(() -> new AcessoNegadoException("Você não foi convidado para este pedido."));
	    }

	    private void exigirPrestadorDoPedido(Pedido pedido, String emailPrestador) {
	        Prestador prestador = pedido.getPrestador();
	        if (prestador == null || !prestador.getEmail().equals(emailPrestador)) {
	            throw new AcessoNegadoException("Somente o prestador do pedido pode executar esta ação.");
	        }
	    }

	    private void expirarConvitesPendentes(Pedido pedido) {
	        for (ConvitePedido c : pedido.getConvites()) {
	            if (c.getStatus() == StatusConvite.ENVIADO) {
	                c.setStatus(StatusConvite.EXPIRADO);
	                c.setRespondidoEm(LocalDateTime.now());
	            }
	        }
	    }

	    /** Pode ver o pedido: o cliente dono, o prestador que aceitou, um prestador convidado, ou um administrador. */
	    private void exigirAcesso(Pedido pedido, String email) {
	        boolean participante = pedido.getCliente().getEmail().equals(email)
	                || (pedido.getPrestador() != null && pedido.getPrestador().getEmail().equals(email))
	                || pedido.getConvites().stream().anyMatch(c -> c.getPrestador().getEmail().equals(email));
	        if (participante) {
	            return;
	        }
	        boolean admin = usuarioRepository.findByEmail(email)
	                .map(u -> u.getPerfil() == Perfil.ADMIN)
	                .orElse(false);
	        if (!admin) {
	            throw new AcessoNegadoException("Você não tem acesso a este pedido.");
	        }
	    }

	    // ================= conversões =================

	    private EnderecoResponse toEnderecoResponse(Endereco e) {
	        return (e == null) ? null
	                : new EnderecoResponse(e.getRua(), e.getNumero(), e.getCep(), e.getCidade(),
	                        e.getLatitude(), e.getLongitude());
	    }

	    private ConviteResponse toConviteResponse(ConvitePedido convite) {
	        Pedido p = convite.getPedido();
	        return new ConviteResponse(
	                p.getId(),
	                p.getServico().getNome(),
	                p.getDescricao(),
	                p.getValorProposto(),
	                p.getModalidade(),
	                p.isUrgente(),
	                p.getDataHoraServico(),
	                toEnderecoResponse(p.getEndereco()),
	                convite.getEnviadoEm(),
	                viabilidadeService.calcular(p, convite.getPrestador()));
	    }

	    private PedidoResponse toResponse(Pedido p) {
	        Prestador prestador = p.getPrestador();
	        return new PedidoResponse(
	                p.getId(),
	                p.getStatus(),
	                p.getServico().getId(),
	                p.getServico().getNome(),
	                p.getCliente().getId(),
	                p.getCliente().getNome(),
	                prestador == null ? null : prestador.getId(),
	                prestador == null ? null : prestador.getNome(),
	                p.getDescricao(),
	                p.getValorProposto(),
	                p.getValorFinal(),
	                p.getModalidade(),
	                p.isUrgente(),
	                p.getDataHoraServico(),
	                p.getCriadoEm(),
	                toEnderecoResponse(p.getEndereco()),
	                p.getFotos().stream().map(FotoPedido::getUrl).toList());
	    }
	}