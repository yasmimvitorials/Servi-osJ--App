package br.com.servicosja.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByCategoriaIdOrderByNomeAsc(Long categoriaId);

    boolean existsByNomeIgnoreCaseAndCategoriaId(String nome, Long categoriaId);

    boolean existsByCategoriaId(Long categoriaId);
}
