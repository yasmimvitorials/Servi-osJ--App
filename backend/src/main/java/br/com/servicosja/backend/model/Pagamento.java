package br.com.servicosja.backend.model;

import java.math.BigDecimal;

import com.servicoja.model.enums.FormaPagamento;
import com.servicoja.model.enums.StatusPagamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pagamentos")

public class Pagamento {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @OneToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "pedido_id", unique = true)
	    private Pedido pedido;

	    @Column(nullable = false, precision = 10, scale = 2)
	    private BigDecimal valor;

	    @Column(name = "comissao_plataforma", precision = 10, scale = 2)
	    private BigDecimal comissaoPlataforma;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    private FormaPagamento forma;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    private StatusPagamento status = StatusPagamento.PENDENTE;

	    //ID da transação no gateway de pagamento externo. //
	    @Column(name = "id_transacao_externo")
	    private String idTransacaoExterno;

	    public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }
	    public Pedido getPedido() { return pedido; }
	    public void setPedido(Pedido pedido) { this.pedido = pedido; }
	    public BigDecimal getValor() { return valor; }
	    public void setValor(BigDecimal valor) { this.valor = valor; }
	    public BigDecimal getComissaoPlataforma() { return comissaoPlataforma; }
	    public void setComissaoPlataforma(BigDecimal comissaoPlataforma) { this.comissaoPlataforma = comissaoPlataforma; }
	    public FormaPagamento getForma() { return forma; }
	    public void setForma(FormaPagamento forma) { this.forma = forma; }
	    public StatusPagamento getStatus() { return status; }
	    public void setStatus(StatusPagamento status) { this.status = status; }
	    public String getIdTransacaoExterno() { return idTransacaoExterno; }
	    public void setIdTransacaoExterno(String idTransacaoExterno) { this.idTransacaoExterno = idTransacaoExterno; }
	}
