package br.com.servicosja.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.servicosja.backend.model.Mensagem;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByPedidoIdAndIdGreaterThanOrderByIdAsc(Long pedidoId, Long aposId);
}