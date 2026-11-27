package br.com.servicosja.backend.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.servicosja.backend.dto.DisponibilidadeRequest;
import br.com.servicosja.backend.dto.DisponibilidadeResponse;
import br.com.servicosja.backend.dto.PrestadorServicoRequest;
import br.com.servicosja.backend.dto.PrestadorServicoResponse;
import br.com.servicosja.backend.exception.RecursoNaoEncontradoException;
import br.com.servicosja.backend.exception.RegraNegocioException;
import br.com.servicosja.backend.model.Categoria;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.PrestadorServico;
import br.com.servicosja.backend.model.Servico;
import br.com.servicosja.backend.model.enums.DiaSemana;
import br.com.servicosja.backend.repository.PrestadorRepository;
import br.com.servicosja.backend.repository.ServicoRepository;

@ExtendWith(MockitoExtension.class)

public class PrestadorServiceTest {
	  private static final String EMAIL = "joao@email.com";

	    @Mock
	    private PrestadorRepository prestadorRepository;

	    @Mock
	    private ServicoRepository servicoRepository;

	    @InjectMocks
	    private PrestadorService prestadorService;

	    private Prestador prestador() {
	        Prestador p = new Prestador();
	        p.setId(1L);
	        p.setEmail(EMAIL);
	        return p;
	    }

	    private Servico servico(Long id, String nome) {
	        Categoria c = new Categoria();
	        c.setId(1L);
	        c.setNome("Pintura");
	        Servico s = new Servico();
	        s.setId(id);
	        s.setNome(nome);
	        s.setCategoria(c);
	        return s;
	    }

	    private DisponibilidadeRequest horario(DiaSemana dia, int inicio, int fim) {
	        return new DisponibilidadeRequest(dia, LocalTime.of(inicio, 0), LocalTime.of(fim, 0));
	    }

	    // ---------- serviços ----------

	    @Test
	    @DisplayName("Prestador adiciona um serviço do catálogo")
	    void adicionarServico_deveAdicionar() {
	        Prestador p = prestador();
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));
	        when(servicoRepository.findById(10L)).thenReturn(Optional.of(servico(10L, "Pintura de parede")));

	        PrestadorServicoResponse resposta = prestadorService.adicionarServico(EMAIL,
	                new PrestadorServicoRequest(10L, new BigDecimal("150.00")));

	        assertEquals(1, p.getServicos().size());
	        assertEquals(10L, resposta.servicoId());
	        assertEquals("Pintura", resposta.categoriaNome());
	        assertEquals(new BigDecimal("150.00"), resposta.precoBase());
	    }

	    @Test
	    @DisplayName("Não permite oferecer o mesmo serviço duas vezes")
	    void adicionarServico_duplicado_deveLancarExcecao() {
	        Prestador p = prestador();
	        Servico s = servico(10L, "Pintura de parede");
	        PrestadorServico existente = new PrestadorServico();
	        existente.setServico(s);
	        p.getServicos().add(existente);
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));
	        when(servicoRepository.findById(10L)).thenReturn(Optional.of(s));

	        assertThrows(RegraNegocioException.class,
	                () -> prestadorService.adicionarServico(EMAIL, new PrestadorServicoRequest(10L, null)));
	        assertEquals(1, p.getServicos().size());
	    }

	    @Test
	    @DisplayName("Serviço inexistente no catálogo lança 'não encontrado'")
	    void adicionarServico_inexistente_deveLancarExcecao() {
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(prestador()));
	        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

	        assertThrows(RecursoNaoEncontradoException.class,
	                () -> prestadorService.adicionarServico(EMAIL, new PrestadorServicoRequest(99L, null)));
	    }

	    @Test
	    @DisplayName("Remove um serviço que o prestador oferece")
	    void removerServico_deveRemover() {
	        Prestador p = prestador();
	        PrestadorServico ps = new PrestadorServico();
	        ps.setServico(servico(10L, "Pintura de parede"));
	        p.getServicos().add(ps);
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));

	        prestadorService.removerServico(EMAIL, 10L);

	        assertTrue(p.getServicos().isEmpty());
	    }

	    @Test
	    @DisplayName("Remover serviço que o prestador não oferece lança 'não encontrado'")
	    void removerServico_naoOferecido_deveLancarExcecao() {
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(prestador()));

	        assertThrows(RecursoNaoEncontradoException.class, () -> prestadorService.removerServico(EMAIL, 10L));
	    }

	    @Test
	    @DisplayName("Usuário que não é prestador lança 'não encontrado'")
	    void listarServicos_semPrestador_deveLancarExcecao() {
	        when(prestadorRepository.findByEmail("cliente@email.com")).thenReturn(Optional.empty());

	        assertThrows(RecursoNaoEncontradoException.class, () -> prestadorService.listarServicos("cliente@email.com"));
	    }

	    // ---------- disponibilidade ----------

	    @Test
	    @DisplayName("Define a agenda semanal substituindo a anterior")
	    void definirDisponibilidade_deveSubstituirAgenda() {
	        Prestador p = prestador();
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));

	        prestadorService.definirDisponibilidade(EMAIL, List.of(horario(DiaSemana.SEGUNDA, 8, 12)));
	        List<DisponibilidadeResponse> resposta = prestadorService.definirDisponibilidade(EMAIL,
	                List.of(horario(DiaSemana.TERCA, 8, 12), horario(DiaSemana.QUARTA, 13, 17)));

	        assertEquals(2, resposta.size());
	        assertEquals(2, p.getDisponibilidades().size());
	        assertEquals(DiaSemana.TERCA, p.getDisponibilidades().get(0).getDiaSemana());
	    }

	    @Test
	    @DisplayName("Aceita dois turnos no mesmo dia sem sobreposição")
	    void definirDisponibilidade_doisTurnos_devePermitir() {
	        Prestador p = prestador();
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));

	        prestadorService.definirDisponibilidade(EMAIL,
	                List.of(horario(DiaSemana.SEXTA, 13, 17), horario(DiaSemana.SEXTA, 8, 12)));

	        assertEquals(2, p.getDisponibilidades().size());
	    }

	    @Test
	    @DisplayName("Recusa horário com início igual ou depois do fim")
	    void definirDisponibilidade_inicioDepoisDoFim_deveLancarExcecao() {
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(prestador()));

	        assertThrows(RegraNegocioException.class,
	                () -> prestadorService.definirDisponibilidade(EMAIL, List.of(horario(DiaSemana.SEGUNDA, 12, 8))));
	    }

	    @Test
	    @DisplayName("Recusa horários sobrepostos no mesmo dia")
	    void definirDisponibilidade_sobreposto_deveLancarExcecao() {
	        Prestador p = prestador();
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));

	        assertThrows(RegraNegocioException.class, () -> prestadorService.definirDisponibilidade(EMAIL,
	                List.of(horario(DiaSemana.SEGUNDA, 8, 12), horario(DiaSemana.SEGUNDA, 11, 15))));
	        assertTrue(p.getDisponibilidades().isEmpty());
	    }

	    // ---------- raio ----------

	    @Test
	    @DisplayName("Atualiza o raio de atuação")
	    void atualizarRaio_deveAtualizar() {
	        Prestador p = prestador();
	        when(prestadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(p));

	        prestadorService.atualizarRaio(EMAIL, 25.0);

	        assertEquals(25.0, p.getRaioAtuacaoKm());
	    }
	}
