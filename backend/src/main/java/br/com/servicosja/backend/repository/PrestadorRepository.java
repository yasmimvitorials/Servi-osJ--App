package br.com.servicosja.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.servicosja.backend.model.Prestador;

public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    Optional<Prestador> findByEmail(String email);

    // prestadores ativos que oferecem o serviço informado. //
    @Query("select distinct p from Prestador p join p.servicos ps "
            + "where ps.servico.id = :servicoId and ps.ativo = true and p.ativo = true")
    List<Prestador> findAtivosQueOferecem(@Param("servicoId") Long servicoId);
}