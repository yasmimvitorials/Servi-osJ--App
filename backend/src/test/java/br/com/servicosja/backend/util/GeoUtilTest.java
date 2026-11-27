package br.com.servicosja.backend.util;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
public class GeoUtilTest {
	@Test
    @DisplayName("Distância entre o mesmo ponto é zero")
    void distancia_mesmoPonto_deveSerZero() {
        assertEquals(0.0, GeoUtil.distanciaKm(-9.78, -36.09, -9.78, -36.09), 0.0001);
    }

    @Test
    @DisplayName("0,045 grau de latitude equivale a cerca de 5 km")
    void distancia_umaCasaDecimalDeLatitude_deveSerCercaDeCincoKm() {
        assertEquals(5.0, GeoUtil.distanciaKm(-9.78, -36.09, -9.735, -36.09), 0.05);
    }

    @Test
    @DisplayName("A distância é a mesma nos dois sentidos")
    void distancia_deveSerSimetrica() {
        double ida = GeoUtil.distanciaKm(-9.78, -36.09, -9.66, -35.73);
        double volta = GeoUtil.distanciaKm(-9.66, -35.73, -9.78, -36.09);
        assertEquals(ida, volta, 0.0001);
    }
}
