package br.com.servicosja.backend.model;

import com.servicoja.model.enums.Perfil;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

//Usuário do painel web administrativo (gerencia categorias e serviços). //
@Entity
@DiscriminatorValue("ADMIN")

public class Administrador extends Usuario {
	   public Administrador() {
	        setPerfil(Perfil.ADMIN);
	    }
	}
