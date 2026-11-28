package br.com.servicosja.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.ConvitePedido;
import br.com.servicosja.backend.model.enums.StatusConvite;
import br.com.servicosja.backend.model.enums.StatusPedido;

public interface ConvitePedidoRepository  extends JpaRepository<ConvitePedido, Long>  {
	List<ConvitePedido> findByPrestadorEmailAndStatusAndPedidoStatusOrderByEnviadoEmDesc(
            String email, StatusConvite status, StatusPedido statusPedido);
}