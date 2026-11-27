package br.com.servicosja.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.servicosja.backend.dto.AtualizarPerfilRequest;
import br.com.servicosja.backend.dto.EnderecoRequest;
import br.com.servicosja.backend.dto.EnderecoResponse;
import br.com.servicosja.backend.dto.PerfilResponse;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.model.Endereco;
import br.com.servicosja.backend.model.Usuario;
import br.com.servicosja.backend.repository.UsuarioRepository;

/**
 * Dados do próprio usuário logado (cliente, prestador ou admin).
 * Os métodos recebem o e-mail do token JWT: assim ninguém altera os dados de outra pessoa.
 */
@Service


public class UsuarioService {
	private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obterPerfil(String email) {
        return toResponse(buscar(email));
    }

    @Transactional
    public PerfilResponse atualizarDados(String email, AtualizarPerfilRequest request) {
        Usuario usuario = buscar(email);
        usuario.setNome(request.nome().trim());
        usuario.setTelefone(request.telefone().trim());
        return toResponse(usuario);
    }

    @Transactional
    public PerfilResponse atualizarEndereco(String email, EnderecoRequest request) {
        Usuario usuario = buscar(email);

        Endereco endereco = (usuario.getEndereco() != null) ? usuario.getEndereco() : new Endereco();
        endereco.setRua(request.rua().trim());
        endereco.setNumero(request.numero());
        endereco.setCep(request.cep() == null ? null : request.cep().replaceAll("\\D", ""));
        endereco.setCidade(request.cidade().trim());
        endereco.setLatitude(request.latitude());
        endereco.setLongitude(request.longitude());

        usuario.setEndereco(endereco);
        return toResponse(usuario);
    }

    private Usuario buscar(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private PerfilResponse toResponse(Usuario usuario) {
        Endereco e = usuario.getEndereco();
        EnderecoResponse endereco = (e == null) ? null
                : new EnderecoResponse(e.getRua(), e.getNumero(), e.getCep(), e.getCidade(),
                        e.getLatitude(), e.getLongitude());
        return new PerfilResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getTelefone(), usuario.getPerfil(), endereco);
    }
}