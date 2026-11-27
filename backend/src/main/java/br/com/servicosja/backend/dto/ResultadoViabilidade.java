package br.com.servicosja.backend.dto;

import java.math.BigDecimal;

public record ResultadoViabilidade(  boolean viavel,
        double distanciaKm,
        int tempoEstimadoMin,
        BigDecimal custoDeslocamento,
        BigDecimal valorLiquido,
        String motivo) {

    public static ResultadoViabilidade inviavel(String motivo) {
        return new ResultadoViabilidade(false, 0.0, 0, BigDecimal.ZERO, BigDecimal.ZERO, motivo);
    }
}
