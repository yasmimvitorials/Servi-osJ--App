package br.com.servicosja.backend.controller;


import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.servicosja.backend.dto.ConcluirPedidoRequest;
import br.com.servicosja.backend.dto.ConviteResponse;
import br.com.servicosja.backend.dto.PedidoRequest;
import br.com.servicosja.backend.dto.PedidoResponse;
import br.com.servicosja.backend.service.PedidoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/pedidos")

public class PedidoController {

	private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // ----- cliente -----

    @PostMapping
    public ResponseEntity<PedidoResponse> criarPedido(Principal principal, @Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(principal.getName(), request));
    }

    @PutMapping("/{id}/cancelar")
    public PedidoResponse cancelarPedido(Principal principal, @PathVariable Long id) {
        return pedidoService.cancelarPedido(principal.getName(), id);
    }

    // ----- consultas -----

    @GetMapping("/meus")
    public List<PedidoResponse> listarMeus(Principal principal) {
        return pedidoService.listarMeus(principal.getName());
    }

    @GetMapping("/{id}")
    public PedidoResponse obterPedido(Principal principal, @PathVariable Long id) {
        return pedidoService.obterPedido(principal.getName(), id);
    }

    // ----- prestador -----

    @GetMapping("/convites")
    public List<ConviteResponse> listarConvites(Principal principal) {
        return pedidoService.listarConvites(principal.getName());
    }

    @PutMapping("/{id}/aceitar")
    public PedidoResponse aceitarPedido(Principal principal, @PathVariable Long id) {
        return pedidoService.aceitarPedido(principal.getName(), id);
    }

    @PutMapping("/{id}/recusar")
    public ResponseEntity<Void> recusarConvite(Principal principal, @PathVariable Long id) {
        pedidoService.recusarConvite(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/iniciar")
    public PedidoResponse iniciarPedido(Principal principal, @PathVariable Long id) {
        return pedidoService.iniciarPedido(principal.getName(), id);
    }

    /** O corpo é opcional: {"valorFinal": 180.00}. Sem corpo, vale o valor proposto. */
    @PutMapping("/{id}/concluir")
    public PedidoResponse concluirPedido(Principal principal, @PathVariable Long id,
            @Valid @RequestBody(required = false) ConcluirPedidoRequest request) {
        return pedidoService.concluirPedido(principal.getName(), id, request == null ? null : request.valorFinal());
    }
}
