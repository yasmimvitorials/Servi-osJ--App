package br.com.servicosja.backend.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

//Relação N:N entre Prestador e Servico: quais serviços o prestador oferece e por quanto//
@Entity
@Table(name = "prestador_servicos", uniqueConstraints = @UniqueConstraint(columnNames = { "prestador_id",
		"servico_id" }))

public class PrestadorServiço {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prestador_id")
	private Prestador prestador;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servico_id")
	private Servico servico;

	@Column(name = "preco_base", precision = 10, scale = 2)
	private BigDecimal precoBase;

	@Column(nullable = false)
	private boolean ativo = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Prestador getPrestador() {
		return prestador;
	}

	public void setPrestador(Prestador prestador) {
		this.prestador = prestador;
	}

	public Servico getServico() {
		return servico;
	}

	public void setServico(Servico servico) {
		this.servico = servico;
	}

	public BigDecimal getPrecoBase() {
		return precoBase;
	}

	public void setPrecoBase(BigDecimal precoBase) {
		this.precoBase = precoBase;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}
