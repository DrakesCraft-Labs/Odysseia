package org.metamechanists.odysseia.restart;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El archivo que Star lee para saber que hay que reiniciar.
 *
 * Si el JSON sale roto, Star no lo entiende y el reinicio no ocurre: el jugador ve la cuenta atras,
 * el servidor no vuelve y nadie sabe por que. Un nick con comillas basta para provocarlo.
 */
class RestartRequestTest {

    private static final Instant CUANDO = Instant.parse("2026-08-06T18:30:00Z");

    @Test
    void elFormatoNormalEsElEsperado() {
        assertEquals(
                "{\"solicitado\":\"2026-08-06T18:30:00Z\",\"por\":\"JackStar6677\",\"motivo\":\"mantenimiento\"}",
                RestartRequest.contenido(CUANDO, "JackStar6677", "mantenimiento"));
    }

    @Test
    void unMotivoConComillasNoRompeElJson() {
        String json = RestartRequest.contenido(CUANDO, "Jack", "actualizar el \"core\"");
        assertTrue(json.contains("actualizar el \\\"core\\\""), json);
    }

    @Test
    void lasBarrasSeEscapan() {
        assertTrue(RestartRequest.contenido(CUANDO, "Jack", "ruta C:\\temp").contains("C:\\\\temp"));
    }

    @Test
    void losSaltosDeLineaNoParteElArchivo() {
        String json = RestartRequest.contenido(CUANDO, "Jack", "linea1\nlinea2");
        assertEquals(1, json.lines().count());
    }

    @Test
    void losCaracteresDeControlSeDescartan() {
        assertEquals("ab", RestartRequest.escapar("a\u0000\u0007b"));
    }

    @Test
    void unMotivoNuloNoRevienta() {
        assertEquals("", RestartRequest.escapar(null));
    }
}
