package org.metamechanists.odysseia.chatgames;

import java.text.Normalizer;
import java.util.Locale;

/** A compact challenge always retains its accepted answer and a human explanation for timeout reveal. */
public record ChatGameChallenge(SeasonalGameMode mode, String prompt, String answer, String explanation) {
    public boolean matches(String candidate) { return normalize(answer).equals(normalize(candidate)); }

    /*
     * La barra se descarta a proposito. Una respuesta que empieza por '/' no llega nunca al chat:
     * el cliente la manda como comando y AsyncPlayerChatEvent no dispara, asi que el jugador que
     * sabe la respuesta pierde la ronda. Las preguntas ya piden el comando sin barra, y aqui se
     * tolera de todas formas por si alguna vuelve a colarse. Las tildes se quitan por lo mismo:
     * "Poseidon" y "Poseidón" son la misma respuesta para quien juega.
     */
    static String normalize(String value) {
        if (value == null) return "";
        String limpio = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        while (limpio.startsWith("/")) limpio = limpio.substring(1).trim();
        return Normalizer.normalize(limpio, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
