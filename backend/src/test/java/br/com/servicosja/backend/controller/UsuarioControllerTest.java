package br.com.servicosja.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import br.com.servicosja.backend.dto.EnderecoRequest;
import br.com.servicosja.backend.dto.PerfilResponse;
import br.com.servicosja.backend.model.enums.Perfil;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.UsuarioService;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)

public class UsuarioControllerTest {
	 @Autowired
	    private MockMvc mockMvc;

	    @Autowired
	    private ObjectMapper objectMapper;

	    @MockitoBean
	    private UsuarioService usuarioService;

	    // Dependências do SecurityConfig
	    @MockitoBean
	    private TokenService tokenService;

	    @MockitoBean
	    private UsuarioDetailsService usuarioDetailsService;

	    @Test
	    @DisplayName("Sem login, GET /usuarios/me retorna 401")
	    void obterPerfil_semLogin_deveRetornar401() throws Exception {
	        mockMvc.perform(get("/usuarios/me")).andExpect(status().isUnauthorized());
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("Logado, GET /usuarios/me usa o e-mail do token e retorna 200")
	    void obterPerfil_logado_deveRetornar200() throws Exception {
	        when(usuarioService.obterPerfil("maria@email.com"))
	                .thenReturn(new PerfilResponse(1L, "Maria", "maria@email.com", "82999999999", Perfil.CLIENTE, null));

	        mockMvc.perform(get("/usuarios/me"))
	                .andExpect(status().isOk())
	                .andExpect(jsonPath("$.nome").value("Maria"))
	                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("PUT /usuarios/me/endereco válido retorna 200")
	    void atualizarEndereco_valido_deveRetornar200() throws Exception {
	        when(usuarioService.atualizarEndereco(eq("maria@email.com"), any(EnderecoRequest.class)))
	                .thenReturn(new PerfilResponse(1L, "Maria", "maria@email.com", "82999999999", Perfil.CLIENTE, null));

	        EnderecoRequest request = new EnderecoRequest("Rua A", "10", "57240-000", "São Miguel dos Campos", -9.78, -36.09);

	        mockMvc.perform(put("/usuarios/me/endereco")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isOk());
	    }

	    @Test
	    @WithMockUser(username = "maria@email.com", roles = "CLIENTE")
	    @DisplayName("PUT /usuarios/me/endereco com latitude fora do limite retorna 400")
	    void atualizarEndereco_latitudeInvalida_deveRetornar400() throws Exception {
	        EnderecoRequest request = new EnderecoRequest("Rua A", "10", null, "Maceió", 200.0, -36.09);

	        mockMvc.perform(put("/usuarios/me/endereco")
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));
	    }
	}
