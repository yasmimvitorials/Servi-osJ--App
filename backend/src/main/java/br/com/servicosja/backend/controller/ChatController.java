package br.com.servicosja.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.servicosja.backend.dto.MensagemRequest;
import br.com.servicosja.backend.dto.MensagemResponse;
import br.com.servicosja.backend.service.ChatService;
import jakarta.validation.Valid;

// Chat do pedido. Quem pode ler e escrever é verificado no ChatService (cliente e prestador do pedido). //
@RestController
@RequestMapping("/pedidos/{pedidoId}/mensagens")
public class ChatController {
	private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<MensagemResponse> listar(Principal principal, @PathVariable Long pedidoId,
            @RequestParam(required = false) Long aposId) {
        return chatService.listar(principal.getName(), pedidoId, aposId);
    }

    @PostMapping
    public ResponseEntity<MensagemResponse> enviar(Principal principal, @PathVariable Long pedidoId,
            @Valid @RequestBody MensagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.enviar(principal.getName(), pedidoId, request));
    }
}
