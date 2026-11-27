package br.com.servicosja.backend.controller;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.servicosja.backend.dto.AtualizarPerfilRequest;
import br.com.servicosja.backend.dto.EnderecoRequest;
import br.com.servicosja.backend.dto.PerfilResponse;
import br.com.servicosja.backend.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios/me")

public class UsuarioController {
	private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PerfilResponse obterPerfil(Principal principal) {
        return usuarioService.obterPerfil(principal.getName());
    }

    @PutMapping
    public PerfilResponse atualizarDados(Principal principal, @Valid @RequestBody AtualizarPerfilRequest request) {
        return usuarioService.atualizarDados(principal.getName(), request);
    }

    @PutMapping("/endereco")
    public PerfilResponse atualizarEndereco(Principal principal, @Valid @RequestBody EnderecoRequest request) {
        return usuarioService.atualizarEndereco(principal.getName(), request);
    }
}
