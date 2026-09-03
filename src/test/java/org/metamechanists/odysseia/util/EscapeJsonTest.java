package org.metamechanists.odysseia.util;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.metamechanists.odysseia.Odysseia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El mensaje de baneo de vanilla llega con un salto de linea literal y antes se incrustaba tal cual
 * en el embed, asi que Discord devolvia HTTP 400 y la alerta de moderacion no llegaba a nadie.
 */
class EscapeJsonTest {

    private static String embed(String valor) {
        return "{\"content\":\"" + Odysseia.escapeJson(valor) + "\"}";
    }

    @Test
    void elMensajeDeBaneoConSaltoDeLineaProduceJsonValido() {
        String kickMessage = "You are banned from this server.\nReason: bot no autorizado";
        String payload = embed(kickMessage);

        assertDoesNotThrow(() -> JsonParser.parseString(payload));
        assertEquals(kickMessage,
                JsonParser.parseString(payload).getAsJsonObject().get("content").getAsString());
    }

    @Test
    void losCaracteresDeControlSeEscapanEnLugarDeRomperElPayload() {
        String texto = "tab\there\r\nnulo\u0000 fin\u001b[0m";
        String payload = embed(texto);

        assertDoesNotThrow(() -> JsonParser.parseString(payload));
        assertEquals(texto,
                JsonParser.parseString(payload).getAsJsonObject().get("content").getAsString());
    }

    @Test
    void barrasYComillasSiguenEscapandose() {
        String texto = "ruta C:\\temp y \"comillas\"";
        assertEquals("ruta C:\\\\temp y \\\"comillas\\\"", Odysseia.escapeJson(texto));
        assertEquals(texto,
                JsonParser.parseString(embed(texto)).getAsJsonObject().get("content").getAsString());
    }

    @Test
    void unMotivoLargoSeRecortaAlTopeDeCampoDeDiscord() {
        String motivo = "spam ".repeat(400); // 2000 caracteres

        String recortado = Odysseia.escapeJsonCampo(motivo);

        assertTrue(recortado.length() <= Odysseia.LIMITE_CAMPO_DISCORD,
                "el campo escapado no puede pasar del tope de Discord");
        String decodificado = JsonParser.parseString("{\"content\":\"" + recortado + "\"}")
                .getAsJsonObject().get("content").getAsString();
        assertEquals(Odysseia.LIMITE_CAMPO_DISCORD, decodificado.length());
        assertTrue(decodificado.endsWith("\u2026"), "el corte se marca con puntos suspensivos");
    }

    @Test
    void elMensajeDeBaneoLargoSigueSiendoJsonValido() {
        String kickMessage = "You are banned from this server.\nReason: " + "\tmotivo\r".repeat(300);

        String payload = "{\"content\":\"" + Odysseia.escapeJsonCampo(kickMessage) + "\"}";

        assertDoesNotThrow(() -> JsonParser.parseString(payload));
        // Discord mide el texto ya decodificado, no el JSON escapado.
        String decodificado = JsonParser.parseString(payload)
                .getAsJsonObject().get("content").getAsString();
        assertTrue(decodificado.length() <= Odysseia.LIMITE_CAMPO_DISCORD);
    }

    @Test
    void unTextoQueCabeNoSeToca() {
        String motivo = "uso de cliente no autorizado";
        assertEquals(Odysseia.escapeJson(motivo), Odysseia.escapeJsonCampo(motivo));
    }

    @Test
    void elCorteNoParteUnParSuplente() {
        // El emoji ocupa dos char, asi que el corte cae justo en medio del par.
        String texto = "a".repeat(Odysseia.LIMITE_CAMPO_DISCORD - 2) + "\uD83D\uDE00" + "b".repeat(50);

        String recortado = Odysseia.escapeJsonCampo(texto);

        assertDoesNotThrow(() -> JsonParser.parseString("{\"content\":\"" + recortado + "\"}"));
        assertEquals(-1, recortado.indexOf('\uD83D'), "no debe quedar un suplente alto suelto");
    }

    @Test
    void nullSigueDevolviendoCadenaVacia() {
        assertEquals("", Odysseia.escapeJson(null));
    }

    @Test
    void elTextoNormalNoSeAltera() {
        assertEquals("Sanción aplicada · pacox77", Odysseia.escapeJson("Sanción aplicada · pacox77"));
    }
}
