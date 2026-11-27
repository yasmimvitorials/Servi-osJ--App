package br.com.servicosja.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.servicosja.backend.dto.ResultadoViabilidade;
import br.com.servicosja.backend.model.Endereco;
import br.com.servicosja.backend.model.Pedido;
import br.com.servicosja.backend.model.Prestador;
import br.com.servicosja.backend.model.PrestadorServico;
import br.com.servicosja.backend.util.GeoUtil;

@Service

public class ViabilidadeService {

    private final BigDecimal custoPorKm;
    private final double velocidadeMediaKmh;

    public ViabilidadeService(@Value("${viabilidade.custo-por-km:1.50}") BigDecimal custoPorKm,
                              @Value("${viabilidade.velocidade-media-kmh:30}") double velocidadeMediaKmh) {
        this.custoPorKm = custoPorKm;
        this.velocidadeMediaKmh = velocidadeMediaKmh;
    }

    public ResultadoViabilidade calcular(Pedido pedido, Prestador prestador) {
        Endereco origem = prestador.getEndereco();
        Endereco destino = pedido.getEndereco();
        if (!temCoordenadas(origem) || !temCoordenadas(destino)) {
            return ResultadoViabilidade.inviavel("Endereço sem coordenadas: não é possível calcular a distância.");
        }

        double distancia = arredondar(GeoUtil.distanciaKm(
                origem.getLatitude(), origem.getLongitude(),
                destino.getLatitude(), destino.getLongitude()));
        int tempoMin = (int) Math.ceil(distancia * 60 / velocidadeMediaKmh);

        BigDecimal custo = custoPorKm
                .multiply(BigDecimal.valueOf(distancia))
                .multiply(BigDecimal.valueOf(2))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal proposto = pedido.getValorProposto() != null ? pedido.getValorProposto() : BigDecimal.ZERO;
        BigDecimal liquido = proposto.subtract(custo);
        BigDecimal minimo = precoMinimo(pedido, prestador);

        String motivo = null;
        if (distancia > prestador.getRaioAtuacaoKm()) {
            motivo = String.format(Locale.ROOT, "Distância de %.1f km acima do seu raio de atuação de %.1f km.",
                    distancia, prestador.getRaioAtuacaoKm());
        } else if (liquido.compareTo(minimo) < 0) {
            motivo = String.format(Locale.ROOT, "Valor líquido de R$ %.2f abaixo do seu preço mínimo de R$ %.2f.",
                    liquido, minimo);
        }

        return new ResultadoViabilidade(motivo == null, distancia, tempoMin, custo, liquido, motivo);
    }

    /** Preço base que o prestador definiu para o serviço do pedido (zero se não definiu). */
    private BigDecimal precoMinimo(Pedido pedido, Prestador prestador) {
        Long servicoId = pedido.getServico().getId();
        return prestador.getServicos().stream()
                .filter(ps -> ps.getServico().getId().equals(servicoId))
                .findFirst()
                .map(PrestadorServico::getPrecoBase)
                .orElse(BigDecimal.ZERO);
    }

    private boolean temCoordenadas(Endereco e) {
        return e != null && e.getLatitude() != null && e.getLongitude() != null;
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}