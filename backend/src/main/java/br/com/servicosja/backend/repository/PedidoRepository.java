package br.com.servicosja.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.enums.StatusPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long>  {
	List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    List<Pedido> findByPrestadorIdOrderByCriadoEmDesc(Long prestadorId);

    List<Pedido> findByStatus(StatusPedido status);
}
