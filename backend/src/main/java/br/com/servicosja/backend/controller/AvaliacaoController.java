package br.com.servicosja.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.servicosja.backend.dto.AvaliacaoRequest;
import br.com.servicosja.backend.dto.AvaliacaoResponse;
import br.com.servicosja.backend.service.AvaliacaoService;
import jakarta.validation.Valid;

@RestController
public class AvaliacaoController {
	 private final AvaliacaoService avaliacaoService;

	    public AvaliacaoController(AvaliacaoService avaliacaoService) {
	        this.avaliacaoService = avaliacaoService;
	    }

	    /** Cliente avalia o prestador, ou o prestador avalia o cliente, depois que o pedido é concluído. */
	    @PostMapping("/pedidos/{pedidoId}/avaliacao")
	    public ResponseEntity<AvaliacaoResponse> avaliar(Principal principal, @PathVariable Long pedidoId,
	            @Valid @RequestBody AvaliacaoRequest request) {
	        return ResponseEntity.status(HttpStatus.CREATED)
	                .body(avaliacaoService.avaliar(principal.getName(), pedidoId, request));
	    }

	    /** Avaliações recebidas por um usuário (útil para mostrar a reputação de um prestador). */
	    @GetMapping("/usuarios/{usuarioId}/avaliacoes")
	    public List<AvaliacaoResponse> listarRecebidas(@PathVariable Long usuarioId) {
	        return avaliacaoService.listarRecebidas(usuarioId);
	    }
	}
