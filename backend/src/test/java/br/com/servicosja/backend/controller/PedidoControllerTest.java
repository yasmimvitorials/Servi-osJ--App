package br.com.servicosja.backend.controller;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.servicosja.backend.dto.PedidoRequest;
import br.com.servicosja.backend.dto.PedidoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.model.enums.Modalidade;
import br.com.servicosja.backend.model.enums.StatusPedido;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.PedidoService;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)

public class PedidoControllerTest {

	 @Autowired
	    private MockMvc mockMvc;

	    @Autowired
	    private ObjectMapper objectMapper;

	    @MockitoBean
	    private PedidoService pedidoService;

	    // Dependências do SecurityConfig
	    @MockitoBean
	    private TokenService tokenService;

	    @MockitoBean
	    private UsuarioDetailsService usuarioDetailsService;

	    private PedidoResponse resposta() {
	        return new PedidoResponse(100L, StatusPedido.ABERTO, 10L, "Pintura de parede", 1L, "Maria",
	                null, null, "Pintar a sala", new BigDecimal("100.00"), null, Modalidade.HOJE,
	                false, null, null, null, List.of());
	    }

	    private String jsonPedido(String descricao) throws Exception {
	        PedidoRequest request = new PedidoRequest(10L, descricao, new BigDecimal("100.00"),
	                Modalidade.HOJE, false, null, null, null);
	        return objectMapper.writeValueAsString(request);
	    }

	    @Test
	    @DisplayName("Sem login, criar pedido retorna 401")
	    void criarPedido_semLogin_deveRetornar401() throws Exception {
	        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(jsonPedido("Pintar")))
	                .andExpect(status().isUnauthorized());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Prestador não pode criar pedido (403)")
	    void criarPedido_comoPrestador_deveRetornar403() throws Exception {
	        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(jsonPedido("Pintar")))
	                .andExpect(status().isForbidden());
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("Cliente cria pedido e recebe 201")
	    void criarPedido_comoCliente_deveRetornar201() throws Exception {
	        when(pedidoService.criarPedido(eq("maria@email.com"), any(PedidoRequest.class))).thenReturn(resposta());

	        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(jsonPedido("Pintar a sala")))
	                .andExpect(status().isCreated())
	                .andExpect(jsonPath("$.id").value(100))
	                .andExpect(jsonPath("$.status").value("ABERTO"));
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("Pedido sem descrição retorna 400")
	    void criarPedido_semDescricao_deveRetornar400() throws Exception {
	        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content(jsonPedido("")))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("GET /pedidos/meus retorna a lista do usuário logado")
	    void listarMeus_deveRetornar200() throws Exception {
	        when(pedidoService.listarMeus("maria@email.com")).thenReturn(List.of(resposta()));

	        mockMvc.perform(get("/pedidos/meus"))
	                .andExpect(status().isOk())
	                .andExpect(jsonPath("$[0].id").value(100));
	    }

	    @Test
	    @WithMockUser(username = "outro@email.com", roles = "CLIENTE")
	    @DisplayName("Acesso negado do service vira 403 com mensagem")
	    void obterPedido_acessoNegado_deveRetornar403() throws Exception {
	        when(pedidoService.obterPedido("outro@email.com", 100L))
	                .thenThrow(new AcessoNegadoException("Você não tem acesso a este pedido."));

	        mockMvc.perform(get("/pedidos/100"))
	                .andExpect(status().isForbidden())
	                .andExpect(jsonPath("$.mensagem").value("Você não tem acesso a este pedido."));
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("PUT /pedidos/{id}/cancelar retorna 200")
	    void cancelar_deveRetornar200() throws Exception {
	        when(pedidoService.cancelarPedido("maria@email.com", 100L)).thenReturn(resposta());

	        mockMvc.perform(put("/pedidos/100/cancelar")).andExpect(status().isOk());
	    }

	    // ---------- parte 2: rotas do prestador ----------

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Prestador lista seus convites (200)")
	    void listarConvites_comoPrestador_deveRetornar200() throws Exception {
	        when(pedidoService.listarConvites("joao@email.com")).thenReturn(List.of());

	        mockMvc.perform(get("/pedidos/convites")).andExpect(status().isOk());
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("Cliente não acessa a lista de convites (403)")
	    void listarConvites_comoCliente_deveRetornar403() throws Exception {
	        mockMvc.perform(get("/pedidos/convites")).andExpect(status().isForbidden());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Prestador aceita o pedido (200)")
	    void aceitar_comoPrestador_deveRetornar200() throws Exception {
	        when(pedidoService.aceitarPedido("joao@email.com", 100L)).thenReturn(resposta());

	        mockMvc.perform(put("/pedidos/100/aceitar"))
	                .andExpect(status().isOk())
	                .andExpect(jsonPath("$.id").value(100));
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("Cliente não pode aceitar pedido (403)")
	    void aceitar_comoCliente_deveRetornar403() throws Exception {
	        mockMvc.perform(put("/pedidos/100/aceitar")).andExpect(status().isForbidden());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Recusar convite retorna 204")
	    void recusar_deveRetornar204() throws Exception {
	        mockMvc.perform(put("/pedidos/100/recusar")).andExpect(status().isNoContent());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Iniciar pedido retorna 200")
	    void iniciar_deveRetornar200() throws Exception {
	        when(pedidoService.iniciarPedido("joao@email.com", 100L)).thenReturn(resposta());

	        mockMvc.perform(put("/pedidos/100/iniciar")).andExpect(status().isOk());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Concluir sem corpo é permitido (valor final opcional)")
	    void concluir_semCorpo_deveRetornar200() throws Exception {
	        when(pedidoService.concluirPedido(eq("joao@email.com"), eq(100L), any())).thenReturn(resposta());

	        mockMvc.perform(put("/pedidos/100/concluir")).andExpect(status().isOk());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Concluir com valor final zero retorna 400")
	    void concluir_valorInvalido_deveRetornar400() throws Exception {
	        mockMvc.perform(put("/pedidos/100/concluir")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content("{\"valorFinal\": 0}"))
	                .andExpect(status().isBadRequest());
	    }
	}
