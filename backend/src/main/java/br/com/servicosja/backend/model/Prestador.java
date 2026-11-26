package br.com.servicosja.backend.model;

import java.util.ArrayList;
import java.util.List;

import com.servicoja.model.enums.Perfil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
@DiscriminatorValue("PRESTADOR")
public class Prestador extends Usuario {
	 @Column(name = "raio_atuacao_km")
	    private double raioAtuacaoKm = 10.0;

	    @Column(name = "nota_media")
	    private double notaMedia = 0.0;

	    @OneToMany(mappedBy = "prestador", cascade = CascadeType.ALL, orphanRemoval = true)
	    private List<PrestadorServico> servicos = new ArrayList<>();

	    @OneToMany(mappedBy = "prestador", cascade = CascadeType.ALL, orphanRemoval = true)
	    private List<Disponibilidade> disponibilidades = new ArrayList<>();

	    public Prestador() {
	        setPerfil(Perfil.PRESTADOR);
	    }

	    public double getRaioAtuacaoKm() { return raioAtuacaoKm; }
	    public void setRaioAtuacaoKm(double raioAtuacaoKm) { this.raioAtuacaoKm = raioAtuacaoKm; }
	    public double getNotaMedia() { return notaMedia; }
	    public void setNotaMedia(double notaMedia) { this.notaMedia = notaMedia; }
	    public List<PrestadorServico> getServicos() { return servicos; }
	    public void setServicos(List<PrestadorServico> servicos) { this.servicos = servicos; }
	    public List<Disponibilidade> getDisponibilidades() { return disponibilidades; }
	    public void setDisponibilidades(List<Disponibilidade> disponibilidades) { this.disponibilidades = disponibilidades; }
	}
