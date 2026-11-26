package br.com.servicosja.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// Guarda a referência ao canal de chat criado no provedor da API externa//
@Entity
@Table(name = "conversas")

public class Conversa {
	  @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @OneToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "pedido_id", unique = true)
	    private Pedido pedido;

	    @Column(name = "canal_externo_id", nullable = false)
	    private String canalExternoId;

	    public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }
	    public Pedido getPedido() { return pedido; }
	    public void setPedido(Pedido pedido) { this.pedido = pedido; }
	    public String getCanalExternoId() { return canalExternoId; }
	    public void setCanalExternoId(String canalExternoId) { this.canalExternoId = canalExternoId; }
	}
