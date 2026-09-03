package org.metamechanists.odysseia.util;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.metamechanists.odysseia.Odysseia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    void nullSigueDevolviendoCadenaVacia() {
        assertEquals("", Odysseia.escapeJson(null));
    }

    @Test
    void elTextoNormalNoSeAltera() {
        assertEquals("Sanción aplicada · pacox77", Odysseia.escapeJson("Sanción aplicada · pacox77"));
    }
}
