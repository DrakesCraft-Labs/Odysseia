package org.metamechanists.odysseia.purchase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El ritmo con el que se le pide a Tebex que revise entregas pendientes.
 *
 * Lo que se protege aqui son las dos formas de hacerlo mal: pedirlo una vez por cada jugador que
 * entra, y pedirlo tan seguido que Tebex nos limite. Ambas dejarian al siguiente comprador sin su
 * producto, que es justo lo que se venia a arreglar.
 */
class ForcecheckThrottleTest {

    private static final long COOLDOWN = 90_000L;

    @Test
    void laPrimeraEntradaDisparaSinEsperar() {
        var throttle = new ForcecheckThrottle(COOLDOWN);
        assertEquals(0, throttle.registrarEntrada(1_000L));
    }

    @Test
    void variasEntradasSeguidasCompartenUnaSolaRevision() {
        var throttle = new ForcecheckThrottle(COOLDOWN);
        assertEquals(0, throttle.registrarEntrada(1_000L));
        // Nueve personas mas entrando en el mismo segundo no generan nueve llamadas a Tebex.
        for (int i = 0; i < 9; i++) {
            assertEquals(-1, throttle.registrarEntrada(1_000L + i));
        }
    }

    @Test
    void trasDispararSeRespetaElEnfriamiento() {
        var throttle = new ForcecheckThrottle(COOLDOWN);
        throttle.registrarEntrada(0L);
        throttle.registrarDisparo(0L);

        // Alguien entra 30s despues: hay que esperar los 60s que quedan de enfriamiento.
        assertEquals(60_000L, throttle.registrarEntrada(30_000L));
    }

    @Test
    void pasadoElEnfriamientoSeVuelveADispararDeInmediato() {
        var throttle = new ForcecheckThrottle(COOLDOWN);
        throttle.registrarEntrada(0L);
        throttle.registrarDisparo(0L);
        throttle.registrarEntrada(30_000L);
        throttle.registrarDisparo(90_000L);

        assertEquals(0L, throttle.registrarEntrada(200_000L));
    }

    @Test
    void elDisparoLiberaElHuecoParaLaSiguienteEntrada() {
        var throttle = new ForcecheckThrottle(COOLDOWN);
        throttle.registrarEntrada(0L);
        assertTrue(throttle.hayPendiente());
        throttle.registrarDisparo(0L);
        assertFalse(throttle.hayPendiente());
    }

    @Test
    void unCooldownNegativoNoRompeElCalculo() {
        var throttle = new ForcecheckThrottle(-5_000L);
        throttle.registrarEntrada(0L);
        throttle.registrarDisparo(1_000L);
        assertEquals(0L, throttle.registrarEntrada(1_000L));
    }
}
