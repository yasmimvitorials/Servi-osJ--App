package br.com.servicosja.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.servicosja.backend.dto.PagamentoRequest;
import br.com.servicosja.backend.dto.PagamentoResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.model.enums.FormaPagamento;
import br.com.servicosja.backend.model.enums.StatusPagamento;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.PagamentoService;

@WebMvcTest(PagamentoController.class)
@Import(SecurityConfig.class)
public class PagamentoControllerTest {
	@Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagamentoService pagamentoService;

    // Dependências do SecurityConfig
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private PagamentoResponse resposta() {
        return new PagamentoResponse(1L, 100L, new BigDecimal("180.00"), new BigDecimal("18.00"),
                FormaPagamento.PIX, StatusPagamento.PAGO);
    }

    @Test
    @DisplayName("Sem login, pagar retorna 401")
    void pagar_semLogin_deveRetornar401() throws Exception {
        mockMvc.perform(post("/pedidos/100/pagamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forma\": \"PIX\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("POST pagamento válido retorna 201")
    void pagar_deveRetornar201() throws Exception {
        when(pagamentoService.pagar(eq("maria@email.com"), eq(100L), any(PagamentoRequest.class))).thenReturn(resposta());

        mockMvc.perform(post("/pedidos/100/pagamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forma\": \"PIX\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.comissaoPlataforma").value(18.00));
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("Sem a forma de pagamento retorna 400")
    void pagar_semForma_deveRetornar400() throws Exception {
        mockMvc.perform(post("/pedidos/100/pagamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
    @DisplayName("Quem não é o cliente do pedido recebe 403 com mensagem")
    void pagar_porOutroUsuario_deveRetornar403() throws Exception {
        when(pagamentoService.pagar(eq("joao@email.com"), eq(100L), any(PagamentoRequest.class)))
                .thenThrow(new AcessoNegadoException("Somente o cliente do pedido pode pagá-lo."));

        mockMvc.perform(post("/pedidos/100/pagamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"forma\": \"PIX\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Somente o cliente do pedido pode pagá-lo."));
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("GET consulta o pagamento do pedido")
    void consultar_deveRetornar200() throws Exception {
        when(pagamentoService.consultar("maria@email.com", 100L)).thenReturn(resposta());

        mockMvc.perform(get("/pedidos/100/pagamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forma").value("PIX"));
    }
}
