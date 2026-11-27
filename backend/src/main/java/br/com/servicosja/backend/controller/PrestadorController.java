package br.com.servicosja.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.servicosja.backend.dto.DefinirDisponibilidadeRequest;
import br.com.servicosja.backend.dto.DisponibilidadeResponse;
import br.com.servicosja.backend.dto.PrestadorServicoRequest;
import br.com.servicosja.backend.dto.PrestadorServicoResponse;
import br.com.servicosja.backend.dto.RaioRequest;
import br.com.servicosja.backend.service.PrestadorService;
import jakarta.validation.Valid;

/** Configurações do prestador logado. Apenas perfil PRESTADOR (regra no SecurityConfig: /prestadores/me/**). */
@RestController
@RequestMapping("/prestadores/me")

public class PrestadorController {
	 private final PrestadorService prestadorService;

	    public PrestadorController(PrestadorService prestadorService) {
	        this.prestadorService = prestadorService;
	    }

	    @GetMapping("/servicos")
	    public List<PrestadorServicoResponse> listarServicos(Principal principal) {
	        return prestadorService.listarServicos(principal.getName());
	    }

	    @PostMapping("/servicos")
	    public ResponseEntity<PrestadorServicoResponse> adicionarServico(Principal principal,
	            @Valid @RequestBody PrestadorServicoRequest request) {
	        return ResponseEntity.status(HttpStatus.CREATED)
	                .body(prestadorService.adicionarServico(principal.getName(), request));
	    }

	    @DeleteMapping("/servicos/{servicoId}")
	    public ResponseEntity<Void> removerServico(Principal principal, @PathVariable Long servicoId) {
	        prestadorService.removerServico(principal.getName(), servicoId);
	        return ResponseEntity.noContent().build();
	    }

	    @GetMapping("/disponibilidade")
	    public List<DisponibilidadeResponse> listarDisponibilidade(Principal principal) {
	        return prestadorService.listarDisponibilidade(principal.getName());
	    }

	    @PutMapping("/disponibilidade")
	    public List<DisponibilidadeResponse> definirDisponibilidade(Principal principal,
	            @Valid @RequestBody DefinirDisponibilidadeRequest request) {
	        return prestadorService.definirDisponibilidade(principal.getName(), request.horarios());
	    }

	    @PutMapping("/raio")
	    public ResponseEntity<Void> atualizarRaio(Principal principal, @Valid @RequestBody RaioRequest request) {
	        prestadorService.atualizarRaio(principal.getName(), request.raioAtuacaoKm());
	        return ResponseEntity.noContent().build();
	    }
	}