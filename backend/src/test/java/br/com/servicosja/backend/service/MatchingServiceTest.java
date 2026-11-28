package br.com.servicosja.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.servicosja.backend.dto.ResultadoViabilidade;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.Servico;
import br.com.servicosja.backend.repository.PrestadorRepository;

@ExtendWith(MockitoExtension.class)

public class MatchingServiceTest {
	 @Mock
	    private PrestadorRepository prestadorRepository;

	    @Mock
	    private ViabilidadeService viabilidadeService;

	    private MatchingService service(int maxConvites) {
	        return new MatchingService(prestadorRepository, viabilidadeService, maxConvites);
	    }

	    private Pedido pedido(boolean urgente) {
	        Servico s = new Servico();
	        s.setId(10L);
	        Pedido p = new Pedido();
	        p.setServico(s);
	        p.setUrgente(urgente);
	        return p;
	    }

	    private Prestador prestador(Long id, double nota, double raioKm) {
	        Prestador p = new Prestador();
	        p.setId(id);
	        p.setNotaMedia(nota);
	        p.setRaioAtuacaoKm(raioKm);
	        return p;
	    }

	    private ResultadoViabilidade viavel(double distanciaKm) {
	        return new ResultadoViabilidade(true, distanciaKm, 10, new BigDecimal("10.00"), new BigDecimal("90.00"), null);
	    }

	    private ResultadoViabilidade inviavel() {
	        return ResultadoViabilidade.inviavel("Fora do raio.");
	    }

	    @Test
	    @DisplayName("Ordena por pontuação (proximidade e nota) e descarta os inviáveis")
	    void selecionar_deveOrdenarEDescartarInviaveis() {
	        Pedido pedido = pedido(false);
	        Prestador pertoNotaMedia = prestador(1L, 3.0, 10.0);   // prox 0,9 / nota 0,6 -> 0,75
	        Prestador longeNotaAlta = prestador(2L, 5.0, 10.0);    // prox 0,2 / nota 1,0 -> 0,60
	        Prestador inviavel = prestador(3L, 5.0, 10.0);
	        when(prestadorRepository.findAtivosQueOferecem(10L)).thenReturn(List.of(longeNotaAlta, inviavel, pertoNotaMedia));
	        when(viabilidadeService.calcular(any(Pedido.class), org.mockito.ArgumentMatchers.eq(pertoNotaMedia))).thenReturn(viavel(1.0));
	        when(viabilidadeService.calcular(any(Pedido.class), org.mockito.ArgumentMatchers.eq(longeNotaAlta))).thenReturn(viavel(8.0));
	        when(viabilidadeService.calcular(any(Pedido.class), org.mockito.ArgumentMatchers.eq(inviavel))).thenReturn(inviavel());

	        List<Prestador> escolhidos = service(5).selecionarPrestadores(pedido);

	        assertEquals(List.of(pertoNotaMedia, longeNotaAlta), escolhidos);
	    }

	    @Test
	    @DisplayName("Em pedido urgente a proximidade pesa mais que a nota")
	    void selecionar_urgente_priorizaProximidade() {
	        Prestador perto = prestador(1L, 1.0, 10.0);    // prox 0,9 / nota 0,2
	        Prestador longe = prestador(2L, 5.0, 10.0);    // prox 0,2 / nota 1,0
	        when(prestadorRepository.findAtivosQueOferecem(10L)).thenReturn(List.of(longe, perto));
	        when(viabilidadeService.calcular(any(Pedido.class), org.mockito.ArgumentMatchers.eq(perto))).thenReturn(viavel(1.0));
	        when(viabilidadeService.calcular(any(Pedido.class), org.mockito.ArgumentMatchers.eq(longe))).thenReturn(viavel(8.0));

	        // normal: perto = 0,5*0,9 + 0,5*0,2 = 0,55 | longe = 0,5*0,2 + 0,5*1,0 = 0,60  -> longe primeiro
	        assertEquals(longe, service(5).selecionarPrestadores(pedido(false)).get(0));
	        // urgente: perto = 0,7*0,9 + 0,3*0,2 = 0,69 | longe = 0,7*0,2 + 0,3*1,0 = 0,44 -> perto primeiro
	        assertEquals(perto, service(5).selecionarPrestadores(pedido(true)).get(0));
	    }

	    @Test
	    @DisplayName("Limita o número de convites")
	    void selecionar_deveLimitarQuantidade() {
	        Prestador p1 = prestador(1L, 5.0, 10.0);
	        Prestador p2 = prestador(2L, 4.0, 10.0);
	        Prestador p3 = prestador(3L, 3.0, 10.0);
	        when(prestadorRepository.findAtivosQueOferecem(10L)).thenReturn(List.of(p1, p2, p3));
	        when(viabilidadeService.calcular(any(Pedido.class), any(Prestador.class))).thenReturn(viavel(2.0));

	        List<Prestador> escolhidos = service(2).selecionarPrestadores(pedido(false));

	        assertEquals(2, escolhidos.size());
	        assertEquals(p1, escolhidos.get(0));
	    }

	    @Test
	    @DisplayName("Sem candidatos, devolve lista vazia")
	    void selecionar_semCandidatos_deveDevolverVazio() {
	        when(prestadorRepository.findAtivosQueOferecem(10L)).thenReturn(List.of());

	        assertTrue(service(5).selecionarPrestadores(pedido(false)).isEmpty());
	    }
	}
