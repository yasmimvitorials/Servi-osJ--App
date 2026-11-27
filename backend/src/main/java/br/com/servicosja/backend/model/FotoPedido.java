package br.com.servicosja.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fotos_pedido")

public class FotoPedido {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "pedido_id")
	    private Pedido pedido;

	    @Column(nullable = false, length = 500)
	    private String url;

	    public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }
	    public Pedido getPedido() { return pedido; }
	    public void setPedido(Pedido pedido) { this.pedido = pedido; }
	    public String getUrl() { return url; }
	    public void setUrl(String url) { this.url = url; }
	}
