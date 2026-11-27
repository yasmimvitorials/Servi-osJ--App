package br.com.servicosja.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.servicosja.backend.model.Cliente;

public class TokenServiceTest {
	 private static final String SEGREDO = "uma-chave-de-teste-com-pelo-menos-32-caracteres!!";

	    private TokenService tokenService;
	    private Cliente cliente;

	    @BeforeEach
	    void setUp() {
	        tokenService = new TokenService(SEGREDO, 60);
	        cliente = new Cliente();
	        cliente.setEmail("maria@email.com");
	    }

	    @Test
	    @DisplayName("Token gerado é válido e carrega o e-mail do usuário")
	    void gerarToken_deveSerValidoEConterEmail() {
	        String token = tokenService.gerarToken(cliente);

	        assertTrue(tokenService.valido(token));
	        assertEquals("maria@email.com", tokenService.extrairEmail(token));
	    }

	    @Test
	    @DisplayName("Token adulterado é inválido")
	    void tokenAdulterado_deveSerInvalido() {
	        String token = tokenService.gerarToken(cliente);

	        assertFalse(tokenService.valido(token + "x"));
	    }

	    @Test
	    @DisplayName("Texto qualquer não é um token válido")
	    void textoAleatorio_deveSerInvalido() {
	        assertFalse(tokenService.valido("isso-nao-e-um-jwt"));
	    }

	    @Test
	    @DisplayName("Token assinado com outra chave é inválido")
	    void tokenDeOutraChave_deveSerInvalido() {
	        TokenService outro = new TokenService("outra-chave-totalmente-diferente-com-32-chars!!", 60);

	        assertFalse(tokenService.valido(outro.gerarToken(cliente)));
	    }

	    @Test
	    @DisplayName("Token expirado é inválido")
	    void tokenExpirado_deveSerInvalido() {
	        TokenService expiraLogo = new TokenService(SEGREDO, -1);

	        assertFalse(expiraLogo.valido(expiraLogo.gerarToken(cliente)));
	    }
	}
