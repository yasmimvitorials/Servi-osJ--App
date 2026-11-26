package br.com.servicosja.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.servicoja.model.enums.Modalidade;
import com.servicoja.model.enums.StatusPedido;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos")

public class Pedido {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "cliente_id")
	    private Cliente cliente;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "servico_id")
	    private Servico servico;

	    //Nulo enquanto nenhum prestador aceitou o pedido. //
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "prestador_id")
	    private Prestador prestador;

	    //Local onde o serviço será realizado. //
	    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
	    @JoinColumn(name = "endereco_id")
	    private Endereco endereco;

	    @Column(length = 1000)
	    private String descricao;

	    @Column(name = "valor_proposto", precision = 10, scale = 2)
	    private BigDecimal valorProposto;

	    @Column(name = "valor_final", precision = 10, scale = 2)
	    private BigDecimal valorFinal;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    private Modalidade modalidade;

	    @Column(nullable = false)
	    private boolean urgente = false;

	    @Column(name = "data_hora_servico")
	    private LocalDateTime dataHoraServico;

	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    private StatusPedido status = StatusPedido.ABERTO;

	    @Column(name = "criado_em", nullable = false, updatable = false)
	    private LocalDateTime criadoEm;

	    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	    private List<FotoPedido> fotos = new ArrayList<>();

	    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	    private List<ConvitePedido> convites = new ArrayList<>();

	    @PrePersist
	    void aoCriar() {
	        if (criadoEm == null) {
	            criadoEm = LocalDateTime.now();
	        }
	    }

	    public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }
	    public Cliente getCliente() { return cliente; }
	    public void setCliente(Cliente cliente) { this.cliente = cliente; }
	    public Servico getServico() { return servico; }
	    public void setServico(Servico servico) { this.servico = servico; }
	    public Prestador getPrestador() { return prestador; }
	    public void setPrestador(Prestador prestador) { this.prestador = prestador; }
	    public Endereco getEndereco() { return endereco; }
	    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
	    public String getDescricao() { return descricao; }
	    public void setDescricao(String descricao) { this.descricao = descricao; }
	    public BigDecimal getValorProposto() { return valorProposto; }
	    public void setValorProposto(BigDecimal valorProposto) { this.valorProposto = valorProposto; }
	    public BigDecimal getValorFinal() { return valorFinal; }
	    public void setValorFinal(BigDecimal valorFinal) { this.valorFinal = valorFinal; }
	    public Modalidade getModalidade() { return modalidade; }
	    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }
	    public boolean isUrgente() { return urgente; }
	    public void setUrgente(boolean urgente) { this.urgente = urgente; }
	    public LocalDateTime getDataHoraServico() { return dataHoraServico; }
	    public void setDataHoraServico(LocalDateTime dataHoraServico) { this.dataHoraServico = dataHoraServico; }
	    public StatusPedido getStatus() { return status; }
	    public void setStatus(StatusPedido status) { this.status = status; }
	    public LocalDateTime getCriadoEm() { return criadoEm; }
	    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
	    public List<FotoPedido> getFotos() { return fotos; }
	    public void setFotos(List<FotoPedido> fotos) { this.fotos = fotos; }
	    public List<ConvitePedido> getConvites() { return convites; }
	    public void setConvites(List<ConvitePedido> convites) { this.convites = convites; }
	}
