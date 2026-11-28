package br.com.servicosja.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.servicosja.backend.dto.ResultadoViabilidade;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.repository.PrestadorRepository;

/**
 * Prioridade inteligente: escolhe QUAIS prestadores recebem o convite de um pedido.
 *
 * 1. candidatos = prestadores ativos que oferecem o serviço do pedido;
 * 2. descarta os que não passam no filtro de viabilidade (longe demais ou valor que não compensa);
 * 3. ordena por pontuação
 *      - proximidade: 1 (no local) até 0 (na borda do raio de atuação do prestador)
 *      - nota: nota média / 5
 *      - peso da proximidade: 0,5 normalmente e 0,7 em pedidos urgentes (quem está perto atende mais rápido);
 */
@Service

public class MatchingService {
	private static final double PESO_PROXIMIDADE_NORMAL = 0.5;
    private static final double PESO_PROXIMIDADE_URGENTE = 0.7;
    private static final double NOTA_MAXIMA = 5.0;

    private final PrestadorRepository prestadorRepository;
    private final ViabilidadeService viabilidadeService;
    private final int maxConvites;

    public MatchingService(PrestadorRepository prestadorRepository,
                           ViabilidadeService viabilidadeService,
                           @Value("${matching.max-convites:5}") int maxConvites) {
        this.prestadorRepository = prestadorRepository;
        this.viabilidadeService = viabilidadeService;
        this.maxConvites = maxConvites;
    }

    public List<Prestador> selecionarPrestadores(Pedido pedido) {
        double pesoProximidade = pedido.isUrgente() ? PESO_PROXIMIDADE_URGENTE : PESO_PROXIMIDADE_NORMAL;

        return prestadorRepository.findAtivosQueOferecem(pedido.getServico().getId()).stream()
                .map(p -> new Candidato(p, viabilidadeService.calcular(pedido, p)))
                .filter(c -> c.viabilidade().viavel())
                .sorted(Comparator.comparingDouble((Candidato c) -> pontuacao(c, pesoProximidade)).reversed())
                .limit(maxConvites)
                .map(Candidato::prestador)
                .toList();
    }

    private double pontuacao(Candidato c, double pesoProximidade) {
        double raio = c.prestador().getRaioAtuacaoKm();
        double proximidade = raio > 0 ? Math.max(0.0, 1.0 - c.viabilidade().distanciaKm() / raio) : 0.0;
        double qualidade = c.prestador().getNotaMedia() / NOTA_MAXIMA;
        return pesoProximidade * proximidade + (1.0 - pesoProximidade) * qualidade;
    }

    private record Candidato(Prestador prestador, ResultadoViabilidade viabilidade) {
    }
}
