package br.com.servicosja.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.Conversa;

public interface ConversaRepository extends JpaRepository<Conversa, Long>  {

    Optional<Conversa> findByPedidoId(Long pedidoId);
}
