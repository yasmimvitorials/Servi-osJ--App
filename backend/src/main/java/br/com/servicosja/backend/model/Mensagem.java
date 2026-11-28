package br.com.servicosja.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Mensagem do chat de um pedido (conversa entre o cliente e o prestador que aceitou). //
@Entity
@Table(name = "mensagens", indexes = @Index(name = "idx_mensagem_pedido", columnList = "pedido_id, id"))

public class Mensagem {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "pedido_id")
	    private Pedido pedido;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "autor_id")
	    private Usuario autor;

	    @Column(nullable = false, length = 1000)
	    private String texto;

	    @Column(name = "enviada_em", nullable = false)
	    private LocalDateTime enviadaEm = LocalDateTime.now();

	    public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }
	    public Pedido getPedido() { return pedido; }
	    public void setPedido(Pedido pedido) { this.pedido = pedido; }
	    public Usuario getAutor() { return autor; }
	    public void setAutor(Usuario autor) { this.autor = autor; }
	    public String getTexto() { return texto; }
	    public void setTexto(String texto) { this.texto = texto; }
	    public LocalDateTime getEnviadaEm() { return enviadaEm; }
	    public void setEnviadaEm(LocalDateTime enviadaEm) { this.enviadaEm = enviadaEm; }
	}
