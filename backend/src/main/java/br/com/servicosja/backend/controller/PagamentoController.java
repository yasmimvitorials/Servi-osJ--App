package br.com.servicosja.backend.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.servicosja.backend.dto.PagamentoRequest;
import br.com.servicosja.backend.dto.PagamentoResponse;
import br.com.servicosja.backend.service.PagamentoService;
import jakarta.validation.Valid;

// Pagamento (simulado) do pedido. Quem pode pagar e quem pode consultar é verificado no PagamentoService. //
@RestController
@RequestMapping("/pedidos/{pedidoId}/pagamento")

public class PagamentoController {
	 private final PagamentoService pagamentoService;

	    public PagamentoController(PagamentoService pagamentoService) {
	        this.pagamentoService = pagamentoService;
	    }

	    @PostMapping
	    public ResponseEntity<PagamentoResponse> pagar(Principal principal, @PathVariable Long pedidoId,
	            @Valid @RequestBody PagamentoRequest request) {
	        return ResponseEntity.status(HttpStatus.CREATED)
	                .body(pagamentoService.pagar(principal.getName(), pedidoId, request));
	    }

	    @GetMapping
	    public PagamentoResponse consultar(Principal principal, @PathVariable Long pedidoId) {
	        return pagamentoService.consultar(principal.getName(), pedidoId);
	    }
	}
