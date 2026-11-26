package br.com.servicosja.backend.model;

import com.servicoja.model.enums.Perfil;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CLIENTE")

public class Cliente extends Usuario{

	public Cliente() {
        setPerfil(Perfil.CLIENTE);
    }
}
