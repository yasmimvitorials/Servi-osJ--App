package br.com.servicosja.backend.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.servicosja.backend.dto.AuthResponse;
import br.com.servicosja.backend.dto.CadastroRequest;
import br.com.servicosja.backend.dto.LoginRequest;
import br.com.servicosja.backend.exception.CredenciaisInvalidasException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.Usuario;
import br.com.servicosja.backend.model.enums.Perfil;
import br.com.servicosja.backend.repository.UsuarioRepository;
import br.com.servicosja.backend.security.TokenService;

@ExtendWith(MockitoExtension.class)

public class AutenticacaoServiceTest {
	private static final String CPF_VALIDO = "529.982.247-25";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AutenticacaoService autenticacaoService;

    private CadastroRequest cadastro(Perfil perfil) {
        return new CadastroRequest("Maria Silva", CPF_VALIDO, "  Maria@Email.com ", "82999999999", "senha1234", perfil);
    }

    private Cliente clienteSalvo() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Maria Silva");
        c.setEmail("maria@email.com");
        c.setSenhaHash("hash");
        return c;
    }

    // ---------- cadastrar ----------

    @Test
    @DisplayName("Cadastra um cliente: normaliza e-mail/CPF, criptografa a senha e devolve token")
    void cadastrarCliente_deveSalvarEDevolverToken() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("52998224725")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.gerarToken(any(Usuario.class))).thenReturn("token-falso");

        AuthResponse resposta = autenticacaoService.cadastrar(cadastro(Perfil.CLIENTE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario salvo = captor.getValue();

        assertInstanceOf(Cliente.class, salvo);
        assertEquals("maria@email.com", salvo.getEmail());
        assertEquals("52998224725", salvo.getCpf());
        assertEquals("hash-bcrypt", salvo.getSenhaHash());
        assertNotEquals("senha1234", salvo.getSenhaHash());
        assertEquals("token-falso", resposta.token());
        assertEquals(Perfil.CLIENTE, resposta.perfil());
    }

    @Test
    @DisplayName("Cadastra um prestador como instância de Prestador")
    void cadastrarPrestador_deveCriarPrestador() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioRepository.existsByCpf(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.gerarToken(any(Usuario.class))).thenReturn("token");

        AuthResponse resposta = autenticacaoService.cadastrar(cadastro(Perfil.PRESTADOR));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertInstanceOf(Prestador.class, captor.getValue());
        assertEquals(Perfil.PRESTADOR, resposta.perfil());
    }

    @Test
    @DisplayName("Não permite cadastrar ADMIN pela rota pública")
    void cadastrarAdmin_deveLancarExcecao() {
        assertThrows(RegraNegocioException.class, () -> autenticacaoService.cadastrar(cadastro(Perfil.ADMIN)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita e-mail já cadastrado")
    void cadastrarEmailDuplicado_deveLancarExcecao() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> autenticacaoService.cadastrar(cadastro(Perfil.CLIENTE)));

        assertEquals("E-mail já cadastrado.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita CPF já cadastrado")
    void cadastrarCpfDuplicado_deveLancarExcecao() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioRepository.existsByCpf("52998224725")).thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> autenticacaoService.cadastrar(cadastro(Perfil.CLIENTE)));

        assertEquals("CPF já cadastrado.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    @DisplayName("Login com credenciais corretas devolve token")
    void login_comCredenciaisCorretas_deveDevolverToken() {
        Cliente cliente = clienteSalvo();
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("senha1234", "hash")).thenReturn(true);
        when(tokenService.gerarToken(cliente)).thenReturn("token-falso");

        AuthResponse resposta = autenticacaoService.login(new LoginRequest("MARIA@email.com", "senha1234"));

        assertEquals("token-falso", resposta.token());
        assertEquals("Bearer", resposta.tipo());
        assertEquals(1L, resposta.id());
    }

    @Test
    @DisplayName("Login com senha errada lança CredenciaisInvalidasException")
    void login_comSenhaErrada_deveLancarExcecao() {
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(clienteSalvo()));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequest("maria@email.com", "errada")));
        verify(tokenService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Login com e-mail inexistente lança CredenciaisInvalidasException")
    void login_comEmailInexistente_deveLancarExcecao() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequest("x@email.com", "qualquer")));
    }

    @Test
    @DisplayName("Usuário inativo não consegue logar")
    void login_comUsuarioInativo_deveLancarExcecao() {
        Cliente inativo = clienteSalvo();
        inativo.setAtivo(false);
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(inativo));

        assertThrows(CredenciaisInvalidasException.class,
                () -> autenticacaoService.login(new LoginRequest("maria@email.com", "senha1234")));
    }
}
