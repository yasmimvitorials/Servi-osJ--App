package br.com.servicosja.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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

import br.com.servicosja.backend.dto.MensagemRequest;
import br.com.servicosja.backend.dto.MensagemResponse;
import br.com.servicosja.backend.exception.AcessoNegadoException;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.ChatService;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)

public class ChatControllerTest {
	@Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    // Dependências do SecurityConfig
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private MensagemResponse mensagem() {
        return new MensagemResponse(7L, 1L, "Maria", "Posso ir às 14h?", LocalDateTime.of(2026, 11, 27, 10, 20));
    }

    @Test
    @DisplayName("Sem login, o chat retorna 401")
    void listar_semLogin_deveRetornar401() throws Exception {
        mockMvc.perform(get("/pedidos/100/mensagens")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("GET lista as mensagens e repassa o aposId")
    void listar_deveRetornar200() throws Exception {
        when(chatService.listar("maria@email.com", 100L, 5L)).thenReturn(List.of(mensagem()));

        mockMvc.perform(get("/pedidos/100/mensagens").param("aposId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Posso ir às 14h?"))
                .andExpect(jsonPath("$[0].autorNome").value("Maria"));
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("POST envia mensagem e retorna 201")
    void enviar_deveRetornar201() throws Exception {
        when(chatService.enviar(eq("maria@email.com"), eq(100L), any(MensagemRequest.class))).thenReturn(mensagem());

        mockMvc.perform(post("/pedidos/100/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Posso ir às 14h?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("Mensagem vazia retorna 400")
    void enviar_textoVazio_deveRetornar400() throws Exception {
        mockMvc.perform(post("/pedidos/100/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "intruso@email.com", roles = "CLIENTE")
    @DisplayName("Quem não participa do pedido recebe 403 com mensagem")
    void listar_estranho_deveRetornar403() throws Exception {
        when(chatService.listar("intruso@email.com", 100L, null))
                .thenThrow(new AcessoNegadoException("Somente o cliente e o prestador do pedido participam do chat."));

        mockMvc.perform(get("/pedidos/100/mensagens"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Somente o cliente e o prestador do pedido participam do chat."));
    }
}
