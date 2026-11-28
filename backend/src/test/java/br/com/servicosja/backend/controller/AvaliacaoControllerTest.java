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

import br.com.servicosja.backend.dto.AvaliacaoRequest;
import br.com.servicosja.backend.dto.AvaliacaoResponse;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.AvaliacaoService;

@WebMvcTest(AvaliacaoController.class)
@Import(SecurityConfig.class)
public class AvaliacaoControllerTest {
	@Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvaliacaoService avaliacaoService;

    // Dependências do SecurityConfig
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private AvaliacaoResponse resposta() {
        return new AvaliacaoResponse(50L, 100L, 1L, "Maria", 2L, 5, "Ótimo", LocalDateTime.of(2026, 11, 27, 18, 0));
    }

    @Test
    @DisplayName("Sem login, avaliar retorna 401")
    void avaliar_semLogin_deveRetornar401() throws Exception {
        mockMvc.perform(post("/pedidos/100/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\": 5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("POST avaliação válida retorna 201")
    void avaliar_deveRetornar201() throws Exception {
        when(avaliacaoService.avaliar(eq("maria@email.com"), eq(100L), any(AvaliacaoRequest.class))).thenReturn(resposta());

        mockMvc.perform(post("/pedidos/100/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\": 5, \"comentario\": \"Ótimo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.avaliadorNome").value("Maria"));
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("Nota fora de 1 a 5 retorna 400")
    void avaliar_notaInvalida_deveRetornar400() throws Exception {
        mockMvc.perform(post("/pedidos/100/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\": 6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("Sem nota retorna 400")
    void avaliar_semNota_deveRetornar400() throws Exception {
        mockMvc.perform(post("/pedidos/100/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comentario\": \"Sem nota\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
    @DisplayName("GET lista as avaliações recebidas por um usuário")
    void listarRecebidas_deveRetornar200() throws Exception {
        when(avaliacaoService.listarRecebidas(2L)).thenReturn(List.of(resposta()));

        mockMvc.perform(get("/usuarios/2/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comentario").value("Ótimo"));
    }
}
