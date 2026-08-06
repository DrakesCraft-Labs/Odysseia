package org.metamechanists.odysseia.papa;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las reglas del trueque de la Papa de mar.
 *
 * Lo que se protege aqui es que nadie se lleve dos veces un premio de una sola vez, y que la
 * escalera diga la verdad sobre cuanto falta: el nivel mas alto cuesta dos inventarios enteros de
 * papas, y equivocarse en esa cuenta seria una estafa a alguien que lleva meses juntando.
 */
class PapaLadderTest {

    private static PapaTier nivel(String id, int coste, boolean unica) {
        return new PapaTier(id, id, coste, "BAKED_POTATO", List.of(), List.of(), unica);
    }

    private static PapaLadder escalera() {
        return new PapaLadder(List.of(
                nivel("armadura", 4608, true),
                nivel("cosmetico", 8, false),
                nivel("cheque", 64, false),
                nivel("hermes", 1280, false)));
    }

    @Test
    void seOrdenaDeBaratoACaroAunqueLleguenDesordenados() {
        assertEquals(List.of("cosmetico", "cheque", "hermes", "armadura"),
                escalera().todos().stream().map(PapaTier::id).toList());
    }

    @Test
    void soloEstanAlAlcanceLosQuePuedePagar() {
        assertEquals(List.of("cosmetico", "cheque"),
                escalera().alcanzables(100, Set.of()).stream().map(PapaTier::id).toList());
    }

    @Test
    void unPremioDeUnaSolaVezNoSeRepite() {
        var e = escalera();
        var armadura = e.porId("armadura").orElseThrow();
        assertTrue(e.disponible(armadura, 5000, Set.of()));
        assertFalse(e.disponible(armadura, 5000, Set.of("armadura")));
    }

    @Test
    void losRepetiblesSiguenDisponiblesTrasCanjearlos() {
        var e = escalera();
        var cheque = e.porId("cheque").orElseThrow();
        assertTrue(e.disponible(cheque, 64, Set.of("cheque")));
    }

    @Test
    void elSiguienteEsElMasBaratoQueTodaviaNoPuedePagar() {
        assertEquals("hermes", escalera().siguiente(100, Set.of()).orElseThrow().id());
    }

    @Test
    void elSiguienteSaltaLosDeUnaVezYaGastados() {
        // Con 5000 papas solo quedaria la armadura, y si ya la tiene no hay siguiente.
        assertTrue(escalera().siguiente(5000, Set.of("armadura")).isEmpty());
    }

    @Test
    void calculaBienCuantasFaltan() {
        assertEquals(1180, escalera().faltanParaSiguiente(100, Set.of()));
    }

    @Test
    void haberCanjeadoUnRepetibleNoLoSacaDeLaCuenta() {
        // 'hermes' se puede canjear las veces que quiera, asi que sigue siendo la siguiente meta
        // aunque ya se lo haya llevado una vez.
        assertEquals(1180, escalera().faltanParaSiguiente(100, Set.of("hermes")));
    }

    @Test
    void haberCanjeadoElDeUnaVezSiLoSacaDeLaCuenta() {
        // Con 1500 papas ya paso hermes; lo unico por delante era la armadura, y si ya la tiene
        // no le queda nada a lo que aspirar.
        assertEquals(0, escalera().faltanParaSiguiente(1500, Set.of("armadura")));
    }

    @Test
    void sinNadaQueAspirarNoFaltaNada() {
        assertEquals(0, escalera().faltanParaSiguiente(99999, Set.of()));
    }

    // ── El coste, tal y como lo lee el jugador ──────────────────

    @Test
    void elCosteSeExpresaEnStacks() {
        assertEquals("1 papa", nivel("x", 1, false).costeLegible());
        assertEquals("8 papas", nivel("x", 8, false).costeLegible());
        assertEquals("1 stack", nivel("x", 64, false).costeLegible());
        assertEquals("5 stacks", nivel("x", 320, false).costeLegible());
        assertEquals("1 stack y 30", nivel("x", 94, false).costeLegible());
        // Dos inventarios llenos: 2 x 36 x 64.
        assertEquals("72 stacks", nivel("x", 4608, false).costeLegible());
    }

    // ── La deteccion de papas antiguas ──────────────────────────

    @Test
    void elNombreSeComparaSinCodigosDeColor() {
        // En el config va con '&' y en el item con seccion; tienen que dar igual.
        assertEquals(PapaDeMarItem.sinColores("&6&l✦ Papa de mar ✦"),
                PapaDeMarItem.sinColores("§6§l✦ Papa de mar ✦"));
    }

    @Test
    void unNombreDistintoNoSeConfunde() {
        assertFalse(PapaDeMarItem.sinColores("&6Papa de mar")
                .equals(PapaDeMarItem.sinColores("&6Papa de tierra")));
    }
}
