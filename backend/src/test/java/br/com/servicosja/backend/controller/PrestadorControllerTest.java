package br.com.servicosja.backend.controller;

import static org.mockito.ArgumentMatchers.any;
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

import br.com.servicosja.backend.dto.PrestadorServicoRequest;
import br.com.servicosja.backend.dto.PrestadorServicoResponse;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.PrestadorService;

@WebMvcTest(PrestadorController.class)
@Import(SecurityConfig.class)

public class PrestadorControllerTest {
	 @Autowired
	    private MockMvc mockMvc;

	    @MockitoBean
	    private PrestadorService prestadorService;

	    // Dependências do SecurityConfig
	    @MockitoBean
	    private TokenService tokenService;

	    @MockitoBean
	    private UsuarioDetailsService usuarioDetailsService;

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("Cliente não acessa rotas do prestador (403)")
	    void listarServicos_comoCliente_deveRetornar403() throws Exception {
	        mockMvc.perform(get("/prestadores/me/servicos")).andExpect(status().isForbidden());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Prestador lista seus serviços (200)")
	    void listarServicos_comoPrestador_deveRetornar200() throws Exception {
	        when(prestadorService.listarServicos("joao@email.com"))
	                .thenReturn(List.of(new PrestadorServicoResponse(10L, "Pintura de parede", "Pintura", new BigDecimal("150.00"))));

	        mockMvc.perform(get("/prestadores/me/servicos"))
	                .andExpect(status().isOk())
	                .andExpect(jsonPath("$[0].servicoNome").value("Pintura de parede"));
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Prestador adiciona serviço e recebe 201")
	    void adicionarServico_deveRetornar201() throws Exception {
	        when(prestadorService.adicionarServico(any(String.class), any(PrestadorServicoRequest.class)))
	                .thenReturn(new PrestadorServicoResponse(10L, "Pintura de parede", "Pintura", new BigDecimal("150.00")));

	        mockMvc.perform(post("/prestadores/me/servicos")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content("{\"servicoId\": 10, \"precoBase\": 150.00}"))
	                .andExpect(status().isCreated())
	                .andExpect(jsonPath("$.servicoId").value(10));
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Adicionar serviço sem informar o serviço retorna 400")
	    void adicionarServico_semServicoId_deveRetornar400() throws Exception {
	        mockMvc.perform(post("/prestadores/me/servicos")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content("{\"precoBase\": 150.00}"))
	                .andExpect(status().isBadRequest());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Horário sem dia da semana retorna 400 (validação dentro da lista)")
	    void definirDisponibilidade_horarioInvalido_deveRetornar400() throws Exception {
	        mockMvc.perform(put("/prestadores/me/disponibilidade")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content("{\"horarios\": [{\"horaInicio\": \"08:00\", \"horaFim\": \"12:00\"}]}"))
	                .andExpect(status().isBadRequest());
	    }

	    @Test
	    @WithMockUser(username = "joao@email.com", roles = "PRESTADOR")
	    @DisplayName("Raio fora do limite retorna 400")
	    void atualizarRaio_foraDoLimite_deveRetornar400() throws Exception {
	        mockMvc.perform(put("/prestadores/me/raio")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content("{\"raioAtuacaoKm\": 500}"))
	                .andExpect(status().isBadRequest());
	    }
	}
