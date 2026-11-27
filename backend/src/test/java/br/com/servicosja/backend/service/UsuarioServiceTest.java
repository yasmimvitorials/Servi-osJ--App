package br.com.servicosja.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.servicosja.backend.dto.AtualizarPerfilRequest;
import br.com.servicosja.backend.dto.EnderecoRequest;
import br.com.servicosja.backend.dto.PerfilResponse;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.model.Cliente;
import br.com.servicosja.backend.model.Endereco;
import br.com.servicosja.backend.model.enums.Perfil;
import br.com.servicosja.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)

public class UsuarioServiceTest {
	@Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Maria");
        c.setEmail("maria@email.com");
        c.setTelefone("82999999999");
        return c;
    }

    @Test
    @DisplayName("Devolve o perfil do usuário (sem endereço cadastrado)")
    void obterPerfil_deveDevolverDados() {
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(cliente()));

        PerfilResponse perfil = usuarioService.obterPerfil("maria@email.com");

        assertEquals("Maria", perfil.nome());
        assertEquals(Perfil.CLIENTE, perfil.perfil());
        assertNull(perfil.endereco());
    }

    @Test
    @DisplayName("Usuário inexistente lança 'não encontrado'")
    void obterPerfil_inexistente_deveLancarExcecao() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> usuarioService.obterPerfil("x@email.com"));
    }

    @Test
    @DisplayName("Atualiza nome e telefone removendo espaços")
    void atualizarDados_deveAtualizar() {
        Cliente c = cliente();
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(c));

        PerfilResponse perfil = usuarioService.atualizarDados("maria@email.com",
                new AtualizarPerfilRequest("  Maria Souza ", " 82988888888 "));

        assertEquals("Maria Souza", perfil.nome());
        assertEquals("82988888888", c.getTelefone());
    }

    @Test
    @DisplayName("Cria o endereço quando o usuário ainda não tem, guardando só dígitos do CEP")
    void atualizarEndereco_semEndereco_deveCriar() {
        Cliente c = cliente();
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(c));

        PerfilResponse perfil = usuarioService.atualizarEndereco("maria@email.com",
                new EnderecoRequest("Rua A", "10", "57240-000", "São Miguel dos Campos", -9.78, -36.09));

        assertNotNull(c.getEndereco());
        assertEquals("57240000", c.getEndereco().getCep());
        assertEquals(-9.78, perfil.endereco().latitude());
        assertEquals("São Miguel dos Campos", perfil.endereco().cidade());
    }

    @Test
    @DisplayName("Atualiza o mesmo objeto de endereço quando já existe um")
    void atualizarEndereco_comEndereco_deveReaproveitar() {
        Cliente c = cliente();
        Endereco existente = new Endereco();
        existente.setId(5L);
        existente.setRua("Antiga");
        existente.setCidade("Antiga");
        c.setEndereco(existente);
        when(usuarioRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(c));

        usuarioService.atualizarEndereco("maria@email.com",
                new EnderecoRequest("Rua Nova", null, null, "Maceió", -9.66, -35.73));

        assertSame(existente, c.getEndereco());
        assertEquals(5L, c.getEndereco().getId());
        assertEquals("Rua Nova", c.getEndereco().getRua());
        assertNull(c.getEndereco().getCep());
    }
}