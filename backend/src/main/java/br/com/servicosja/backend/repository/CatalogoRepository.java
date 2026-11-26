package br.com.servicosja.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.Categoria;

public interface CatalogoRepository extends JpaRepository<Categoria, Long> {
	 boolean existsByNomeIgnoreCase(String nome);

	    Optional<Categoria> findByNomeIgnoreCase(String nome);
	}
