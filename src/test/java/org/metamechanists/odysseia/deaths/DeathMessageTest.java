package org.metamechanists.odysseia.deaths;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los mensajes de muerte: eleccion, marcadores y rachas. */
class DeathMessageTest {

    /** Azar de mentira que devuelve siempre lo mismo, para que la prueba sea repetible. */
    private static RandomGenerator fijo(int valor) {
        return new RandomGenerator() {
            @Override public long nextLong() { return valor; }
            @Override public int nextInt(int bound) { return Math.min(valor, bound - 1); }
        };
    }

    private static DeathMessageCatalog catalogo() {
        return new DeathMessageCatalog(Map.of(
                "fall", List.of("cayo A", "cayo B", "cayo C"),
                "generico", List.of("murio y ya")));
    }

    @Test
    void eligeDelGrupoQueCorresponde() {
        assertEquals("cayo A", catalogo().elegir("fall", fijo(0)));
    }

    @Test
    void unaCausaSinMensajesCaeAlGenerico() {
        assertEquals("murio y ya", catalogo().elegir("lava", fijo(0)));
    }

    @Test
    void sinGenericoNoDevuelveNadaYSeQuedaElDeVanilla() {
        var solo = new DeathMessageCatalog(Map.of("fall", List.of("cayo")));
        assertNull(solo.elegir("lava", fijo(0)));
    }

    @Test
    void nuncaRepiteElMensajeAnterior() {
        var c = catalogo();
        // Con el azar siempre en 0 se repetiria sin la proteccion.
        String primero = c.elegir("fall", fijo(0));
        String segundo = c.elegir("fall", fijo(0));
        assertNotEquals(primero, segundo, "el mismo chiste dos veces seguidas pierde la gracia");
    }

    @Test
    void ungrupoDeUnSoloMensajeSePuedeRepetir() {
        var c = new DeathMessageCatalog(Map.of("fall", List.of("unico")));
        assertEquals("unico", c.elegir("fall", fijo(0)));
        assertEquals("unico", c.elegir("fall", fijo(0)));
    }

    @Test
    void losMensajesVaciosNoCuentan() {
        var c = new DeathMessageCatalog(Map.of("fall", List.of("", "   ", "bueno")));
        assertEquals("bueno", c.elegir("fall", fijo(0)));
    }

    // ── Que grupo toca ──────────────────────────────────────────

    private static DeathContext ctx(String causa, boolean pvp, boolean jefe, boolean propia) {
        return new DeathContext("Ney", "SUPER Infernal ENDERMAN", "Espada", "world", 64,
                causa, pvp, jefe, propia);
    }

    @Test
    void elJefeMandaSobreLaCausa() {
        // Caerse huyendo de un jefe es una anecdota sobre el jefe, no sobre la caida.
        assertEquals("jefe", ctx("FALL", false, true, false).clave());
    }

    @Test
    void elPvpMandaSobreLaCausaPeroNoSobreElJefe() {
        assertEquals("pvp", ctx("ENTITY_ATTACK", true, false, false).clave());
        assertEquals("jefe", ctx("ENTITY_ATTACK", true, true, false).clave());
    }

    @Test
    void sinNadaEspecialSeUsaLaCausaEnMinusculas() {
        assertEquals("fall", ctx("FALL", false, false, false).clave());
    }

    @Test
    void unaCausaVaciaCaeAlGenerico() {
        assertEquals(DeathMessageCatalog.GENERICO, ctx("", false, false, false).clave());
    }

    // ── Marcadores ──────────────────────────────────────────────

    @Test
    void losMarcadoresSeSustituyen() {
        String texto = ctx("FALL", false, false, false)
                .aplicar("{jugador} cayo en {mundo} a la altura {y} escapando de {asesino}");
        assertEquals("Ney cayo en world a la altura 64 escapando de SUPER Infernal ENDERMAN", texto);
    }

    @Test
    void sinAsesinoSePoneAlgoLegible() {
        var sinNadie = new DeathContext("Ney", "", "", "world", 64, "FALL", false, false, false);
        assertTrue(sinNadie.aplicar("mato: {asesino}").contains("algo sin nombre"));
        assertTrue(sinNadie.aplicar("con: {arma}").contains("sus propias manos"));
    }

    // ── Rachas ──────────────────────────────────────────────────

    @Test
    void cuentaLasMuertesDentroDeLaVentana() {
        var rachas = new DeathStreakTracker(60_000L);
        UUID quien = UUID.randomUUID();
        assertEquals(1, rachas.registrar(quien, 0L));
        assertEquals(2, rachas.registrar(quien, 10_000L));
        assertEquals(3, rachas.registrar(quien, 20_000L));
    }

    @Test
    void lasMuertesViejasSalenDeLaCuenta() {
        var rachas = new DeathStreakTracker(60_000L);
        UUID quien = UUID.randomUUID();
        rachas.registrar(quien, 0L);
        rachas.registrar(quien, 10_000L);
        // Dos minutos despues las anteriores ya no cuentan.
        assertEquals(1, rachas.registrar(quien, 120_000L));
    }

    @Test
    void cadaJugadorLlevaSuPropiaCuenta() {
        var rachas = new DeathStreakTracker(60_000L);
        UUID uno = UUID.randomUUID();
        UUID otro = UUID.randomUUID();
        rachas.registrar(uno, 0L);
        rachas.registrar(uno, 1_000L);
        assertEquals(1, rachas.registrar(otro, 1_000L));
    }

    @Test
    void alDesconectarseSeOlvida() {
        var rachas = new DeathStreakTracker(60_000L);
        UUID quien = UUID.randomUUID();
        rachas.registrar(quien, 0L);
        assertEquals(1, rachas.seguidos());
        rachas.olvidar(quien);
        assertEquals(0, rachas.seguidos());
    }
}
