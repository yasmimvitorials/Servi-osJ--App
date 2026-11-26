package br.com.servicosja.backend.model;

import br.com.servicosja.backend.model.enums.Perfil;

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
