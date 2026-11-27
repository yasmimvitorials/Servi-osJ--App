package br.com.servicosja.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service

public class AutenticacaoService {
	 private final UsuarioRepository usuarioRepository;
	    private final PasswordEncoder passwordEncoder;
	    private final TokenService tokenService;

	    public AutenticacaoService(UsuarioRepository usuarioRepository,
	                               PasswordEncoder passwordEncoder,
	                               TokenService tokenService) {
	        this.usuarioRepository = usuarioRepository;
	        this.passwordEncoder = passwordEncoder;
	        this.tokenService = tokenService;
	    }

	    @Transactional
	    public AuthResponse cadastrar(CadastroRequest request) {
	        if (request.perfil() == Perfil.ADMIN) {
	            throw new RegraNegocioException("Não é permitido cadastrar administradores por esta rota.");
	        }

	        String email = normalizarEmail(request.email());
	        String cpf = request.cpf().replaceAll("\\D", "");

	        if (usuarioRepository.existsByEmail(email)) {
	            throw new RegraNegocioException("E-mail já cadastrado.");
	        }
	        if (usuarioRepository.existsByCpf(cpf)) {
	            throw new RegraNegocioException("CPF já cadastrado.");
	        }

	        Usuario usuario = (request.perfil() == Perfil.PRESTADOR) ? new Prestador() : new Cliente();
	        usuario.setNome(request.nome().trim());
	        usuario.setCpf(cpf);
	        usuario.setEmail(email);
	        usuario.setTelefone(request.telefone());
	        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));

	        Usuario salvo = usuarioRepository.save(usuario);
	        return montarResposta(salvo);
	    }

	    @Transactional(readOnly = true)
	    public AuthResponse login(LoginRequest request) {
	        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(request.email()))
	                .orElseThrow(CredenciaisInvalidasException::new);

	        if (!usuario.isAtivo() || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
	            throw new CredenciaisInvalidasException();
	        }
	        return montarResposta(usuario);
	    }

	    private AuthResponse montarResposta(Usuario usuario) {
	        String token = tokenService.gerarToken(usuario);
	        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getPerfil());
	    }

	    private String normalizarEmail(String email) {
	        return email.trim().toLowerCase();
	    }
	}