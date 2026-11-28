package br.com.servicosja.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.enums.StatusPedido;
import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long>  {
	List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    List<Pedido> findByPrestadorIdOrderByCriadoEmDesc(Long prestadorId);

    List<Pedido> findByStatus(StatusPedido status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarParaAtualizar(@Param("id") Long id);
}
