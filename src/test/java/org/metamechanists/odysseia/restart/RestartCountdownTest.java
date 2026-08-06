package org.metamechanists.odysseia.restart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El reparto de avisos de la cuenta atras.
 *
 * Avisar cada segundo desde el principio es ruido y la gente lo ignora justo cuando importa;
 * avisar solo al final no da tiempo a ponerse a salvo.
 */
class RestartCountdownTest {

    @Test
    void losUltimosCincoSegundosSeAvisanUnoAUno() {
        for (int s = 1; s <= 5; s++) {
            assertTrue(RestartCountdown.debeAnunciar(s), "faltaba el aviso de " + s + "s");
        }
    }

    @Test
    void entreCincoYTreintaSeAvisaCadaDiez() {
        assertTrue(RestartCountdown.debeAnunciar(10));
        assertTrue(RestartCountdown.debeAnunciar(20));
        assertTrue(RestartCountdown.debeAnunciar(30));
        assertFalse(RestartCountdown.debeAnunciar(15));
        assertFalse(RestartCountdown.debeAnunciar(7));
    }

    @Test
    void hastaCincoMinutosSeAvisaCadaMinuto() {
        assertTrue(RestartCountdown.debeAnunciar(60));
        assertTrue(RestartCountdown.debeAnunciar(120));
        assertTrue(RestartCountdown.debeAnunciar(300));
        assertFalse(RestartCountdown.debeAnunciar(90));
    }

    @Test
    void porEncimaDeCincoMinutosSeEspacia() {
        assertTrue(RestartCountdown.debeAnunciar(600));
        assertTrue(RestartCountdown.debeAnunciar(900));
        assertFalse(RestartCountdown.debeAnunciar(660));
    }

    @Test
    void elCeroYLosNegativosNoAnuncian() {
        // El aviso de "0" lo da el propio reinicio; anunciarlo aqui lo duplicaria.
        assertFalse(RestartCountdown.debeAnunciar(0));
        assertFalse(RestartCountdown.debeAnunciar(-1));
    }

    @Test
    void elTiempoSeLeeEnCastellanoYSinDecimales() {
        assertEquals("1 segundo", RestartCountdown.tiempo(1));
        assertEquals("30 segundos", RestartCountdown.tiempo(30));
        assertEquals("1 minuto", RestartCountdown.tiempo(60));
        assertEquals("2 minutos", RestartCountdown.tiempo(120));
        assertEquals("1 minuto y 30s", RestartCountdown.tiempo(90));
        assertEquals("5 minutos", RestartCountdown.tiempo(300));
    }
}
