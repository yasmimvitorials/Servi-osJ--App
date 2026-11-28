package br.com.servicosja.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.servicosja.backend.model.Avaliacao;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByAvaliadoId(Long avaliadoId);

    /** Avaliações recebidas por um usuário, da mais recente para a mais antiga. */
    List<Avaliacao> findByAvaliadoIdOrderByDataDesc(Long avaliadoId);

    boolean existsByPedidoIdAndAvaliadorId(Long pedidoId, Long avaliadorId);

    @Query("select avg(a.nota) from Avaliacao a where a.avaliado.id = :avaliadoId")
    Optional<Double> calcularMediaPorAvaliado(@Param("avaliadoId") Long avaliadoId);
}
