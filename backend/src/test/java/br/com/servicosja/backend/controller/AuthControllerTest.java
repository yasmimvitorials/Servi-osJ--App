package br.com.servicosja.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.servicosja.backend.dto.AuthResponse;
import br.com.servicosja.backend.dto.CadastroRequest;
import br.com.servicosja.backend.dto.LoginRequest;
import br.com.servicosja.backend.exception.CredenciaisInvalidasException;
import br.com.servicosja.backend.model.enums.Perfil;
import br.com.servicosja.backend.security.SecurityConfig;
import br.com.servicosja.backend.security.TokenService;
import br.com.servicosja.backend.security.UsuarioDetailsService;
import br.com.servicosja.backend.service.AutenticacaoService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)

public class AuthControllerTest {
	@Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    // Dependências do SecurityConfig
    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("POST /auth/login com dados corretos retorna 200 e o token")
    void login_deveRetornar200() throws Exception {
        when(autenticacaoService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token-falso", 1L, "Maria", Perfil.CLIENTE));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("maria@email.com", "senha1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-falso"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /auth/login com e-mail inválido retorna 400")
    void login_comEmailInvalido_deveRetornar400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nao-e-email", "senha1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos."));
    }

    @Test
    @DisplayName("POST /auth/login com credenciais erradas retorna 401")
    void login_comCredenciaisInvalidas_deveRetornar401() throws Exception {
        when(autenticacaoService.login(any(LoginRequest.class))).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("maria@email.com", "errada123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("POST /auth/cadastro válido retorna 201")
    void cadastro_deveRetornar201() throws Exception {
        when(autenticacaoService.cadastrar(any(CadastroRequest.class)))
                .thenReturn(new AuthResponse("token-falso", 2L, "João", Perfil.PRESTADOR));

        CadastroRequest request = new CadastroRequest("João", "529.982.247-25", "joao@email.com",
                "82999999999", "senha1234", Perfil.PRESTADOR);

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @DisplayName("POST /auth/cadastro com CPF inválido e senha curta retorna 400 com detalhes")
    void cadastro_comDadosInvalidos_deveRetornar400() throws Exception {
        CadastroRequest request = new CadastroRequest("João", "111.111.111-11", "joao@email.com",
                "82999999999", "123", Perfil.CLIENTE);

        mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes").isArray());
    }
}