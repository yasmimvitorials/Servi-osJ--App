package br.com.servicosja.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.Pagamento;


public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
	 Optional<Pagamento> findByPedidoId(Long pedidoId);
	}
